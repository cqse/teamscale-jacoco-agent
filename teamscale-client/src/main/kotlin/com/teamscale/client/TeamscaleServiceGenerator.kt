package com.teamscale.client

import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.io.IOException
import java.time.Duration

/** Helper class for generating a teamscale compatible service.  */
object TeamscaleServiceGenerator {
	/** Custom user agent of the requests, used to monitor API traffic.  */
	const val USER_AGENT = "Teamscale Java Profiler"

	/**
	 * Generates a [Retrofit] instance for the given service, which uses basic auth to authenticate against the
	 * server and which sets the accept header to json.
	 */
	@JvmStatic
	@JvmOverloads
	fun <S> createService(
		serviceClass: Class<S>,
		baseUrl: HttpUrl,
		username: String,
		accessToken: String,
		readTimeout: Duration = HttpUtils.DEFAULT_READ_TIMEOUT,
		writeTimeout: Duration = HttpUtils.DEFAULT_WRITE_TIMEOUT,
		vararg interceptors: Interceptor
	) = createServiceWithRequestLogging(
		serviceClass, baseUrl, username, accessToken, null, readTimeout, writeTimeout, *interceptors
	)

	/**
	 * Generates a [Retrofit] instance for the given service, which uses basic auth to authenticate against the
	 * server and which sets the accept-header to json. Logs requests and responses to the given logfile.
	 */
	fun <S> createServiceWithRequestLogging(
		serviceClass: Class<S>,
		baseUrl: HttpUrl,
		username: String,
		accessToken: String,
		logfile: File?,
		readTimeout: Duration,
		writeTimeout: Duration,
		vararg interceptors: Interceptor
	): S = HttpUtils.createRetrofit(
		{ retrofitBuilder ->
			retrofitBuilder.baseUrl(baseUrl)
				.addConverterFactory(JacksonConverterFactory.create(JsonUtils.OBJECT_MAPPER))
		},
		{ okHttpBuilder ->
			okHttpBuilder.addInterceptors(*interceptors)
				.addInterceptor(HttpUtils.getBasicAuthInterceptor(username, accessToken))
				.addInterceptor(AcceptJsonInterceptor())
				.addNetworkInterceptor(CustomUserAgentInterceptor())
			logfile?.let { okHttpBuilder.addInterceptor(FileLoggingInterceptor(it)) }
		},
		readTimeout, writeTimeout
	).create(serviceClass)

	private fun OkHttpClient.Builder.addInterceptors(
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
	private class AcceptJsonInterceptor : Interceptor {
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
