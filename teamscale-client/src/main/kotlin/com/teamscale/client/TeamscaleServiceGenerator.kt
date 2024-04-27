package com.teamscale.client

import com.teamscale.client.utils.JsonUtils.OBJECT_MAPPER
import com.teamscale.client.utils.HttpUtils
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.io.IOException
import java.time.Duration

/** Helper class for generating a teamscale compatible service.  */
object TeamscaleServiceGenerator {
	/** Custom user agent of the requests, used to monitor API traffic.  */
	const val USER_AGENT: String = "Teamscale JaCoCo Agent"

	/**
	 * Generates a [Retrofit] instance for the given service, which uses basic auth to authenticate against the
	 * server and which sets the accept-header to json. Logs requests and responses to the given logfile.
	 */
	inline fun <reified S> createServiceWithRequestLogging(
		baseUrl: HttpUrl,
		username: String,
		accessToken: String,
		logfile: File?,
		readTimeout: Duration,
		writeTimeout: Duration,
		vararg interceptors: Interceptor
	): S {
		val retrofit = HttpUtils.createRetrofit(
			{ retrofitBuilder ->
				retrofitBuilder.baseUrl(baseUrl)
					.addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER))
			},
			{ okHttpBuilder ->
				okHttpBuilder.addInterceptors(*interceptors)
					.addInterceptor(HttpUtils.getBasicAuthInterceptor(username, accessToken))
					.addInterceptor(AcceptJsonInterceptor())
					.addNetworkInterceptor(CustomUserAgentInterceptor())
				logfile?.let {
					okHttpBuilder.addInterceptor(FileLoggingInterceptor(it))
				}
			},
			readTimeout, writeTimeout
		)
		return retrofit.create(S::class.java)
	}

	// ToDo: Remove when tests are in Kotlin
	/**
	 * Generates a [Retrofit] instance for the given service, which uses basic auth to authenticate against the
	 * server and which sets the accept-header to json. Logs requests and responses to the given logfile.
	 */
	@JvmOverloads
	@JvmStatic
	fun <S> createServiceWithRequestLogging(
		clazz: Class<S>,
		baseUrl: HttpUrl,
		username: String,
		accessToken: String,
		logfile: File? = null,
		readTimeout: Duration = HttpUtils.DEFAULT_READ_TIMEOUT,
		writeTimeout: Duration = HttpUtils.DEFAULT_WRITE_TIMEOUT,
		vararg interceptors: Interceptor
	): S {
		val retrofit = HttpUtils.createRetrofit(
			{ retrofitBuilder ->
				retrofitBuilder.baseUrl(baseUrl)
					.addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER))
			},
			{ okHttpBuilder ->
				okHttpBuilder.addInterceptors(*interceptors)
					.addInterceptor(HttpUtils.getBasicAuthInterceptor(username, accessToken))
					.addInterceptor(AcceptJsonInterceptor())
					.addNetworkInterceptor(CustomUserAgentInterceptor())
				logfile?.let {
					okHttpBuilder.addInterceptor(FileLoggingInterceptor(it))
				}
			},
			readTimeout, writeTimeout
		)
		return retrofit.create(clazz)
	}

	fun OkHttpClient.Builder.addInterceptors(
		vararg interceptors: Interceptor
	): OkHttpClient.Builder {
		interceptors.forEach { interceptor ->
			addInterceptor(interceptor)
		}
		return this
	}

	/**
	 * Sets an `Accept: application/json` header on all requests.
	 */
	class AcceptJsonInterceptor : Interceptor {
		@Throws(IOException::class)
		override fun intercept(chain: Interceptor.Chain): Response {
			val newRequest = chain.request().newBuilder().header("Accept", "application/json").build()
			return chain.proceed(newRequest)
		}
	}

	/**
	 * Sets the custom user agent [.USER_AGENT] header on all requests.
	 */
	class CustomUserAgentInterceptor : Interceptor {
		@Throws(IOException::class)
		override fun intercept(chain: Interceptor.Chain): Response {
			val newRequest = chain.request().newBuilder().header("User-Agent", USER_AGENT).build()
			return chain.proceed(newRequest)
		}
	}
}
