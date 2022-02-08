package com.teamscale.client;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.io.File;
import java.io.IOException;

/** Helper class for generating a teamscale compatible service. */
public class TeamscaleServiceGenerator {

	/**
	 * Generates a {@link Retrofit} instance for the given service, which uses basic auth to authenticate against the
	 * server and which sets the accept header to json.
	 */
	public static <S> S createService(Class<S> serviceClass, HttpUrl baseUrl, String username, String accessToken,
									  int readTimeout, int writeTimeout, Interceptor... interceptors) {
		Retrofit retrofit = HttpUtils.createRetrofit(
				retrofitBuilder -> retrofitBuilder.baseUrl(baseUrl).addConverterFactory(MoshiConverterFactory.create()),
				okHttpBuilder -> addInterceptors(okHttpBuilder, interceptors)
						.addInterceptor(HttpUtils.getBasicAuthInterceptor(username, accessToken))
						.addInterceptor(new AcceptJsonInterceptor()), readTimeout, writeTimeout
		);
		return retrofit.create(serviceClass);
	}

	/**
	 * Generates a {@link Retrofit} instance for the given service, which uses basic auth to authenticate against the
	 * server and which sets the accept-header to json. Logs requests and responses to the given logfile.
	 */
	public static <S> S createServiceWithRequestLogging(Class<S> serviceClass, HttpUrl baseUrl, String username,
														String accessToken, File logfile, int readTimeout,
														int writeTimeout, Interceptor... interceptors) {
		Retrofit retrofit = HttpUtils.createRetrofit(
				retrofitBuilder -> retrofitBuilder.baseUrl(baseUrl).addConverterFactory(MoshiConverterFactory.create()),
				okHttpBuilder -> addInterceptors(okHttpBuilder, interceptors)
						.addInterceptor(HttpUtils.getBasicAuthInterceptor(username, accessToken))
						.addInterceptor(new AcceptJsonInterceptor())
						.addInterceptor(new FileLoggingInterceptor(logfile)),
				readTimeout, writeTimeout
		);
		return retrofit.create(serviceClass);
	}

	private static OkHttpClient.Builder addInterceptors(OkHttpClient.Builder builder, Interceptor... interceptors) {
		for (Interceptor interceptor : interceptors) {
			builder.addInterceptor(interceptor);
		}
		return builder;
	}


	/**
	 * Sets an <code>Accept: application/json</code> header on all requests.
	 */
	private static class AcceptJsonInterceptor implements Interceptor {

		@Override
		public Response intercept(Chain chain) throws IOException {
			Request newRequest = chain.request().newBuilder().header("Accept", "application/json").build();
			return chain.proceed(newRequest);
		}
	}

}
