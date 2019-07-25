package com.teamscale.client;

import eu.cqse.teamscale.client.HttpUtils;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/** Helper class for generating a teamscale compatible service. */
public class TeamscaleServiceGenerator {

	/**
	 * Generates a {@link Retrofit} instance for the given
	 * service, which uses basic auth to authenticate against the server and which sets the accept header to json.
	 */
	public static <S> S createService(Class<S> serviceClass, HttpUrl baseUrl, String username, String accessToken) {
		Retrofit retrofit = HttpUtils.createRetrofit(
				retrofitBuilder -> retrofitBuilder.baseUrl(baseUrl).addConverterFactory(MoshiConverterFactory.create()),
				okHttpBuilder -> okHttpBuilder
						.addInterceptor(TeamscaleServiceGenerator.getBasicAuthInterceptor(username, accessToken))
						.addInterceptor(new AcceptJsonInterceptor())
		);
		return retrofit.create(serviceClass);
	}
public static <S> S createServiceWithRequestLogging(Class<S> serviceClass, HttpUrl baseUrl, String username,
														String password, File file) {
		Retrofit retrofit = HttpUtils.createRetrofit(
				retrofitBuilder -> retrofitBuilder.baseUrl(baseUrl).addConverterFactory(MoshiConverterFactory.create()),
				okHttpBuilder -> okHttpBuilder
						.addInterceptor(TeamscaleServiceGenerator.getBasicAuthInterceptor(username, accessToken))
						.addInterceptor(new AcceptJsonInterceptor()
						.addInterceptor(new FileLoggingInterceptor(file)))
		);
		return retrofit.create(serviceClass);
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

	/**
	 * Returns an interceptor, which adds a basic auth header to a request.
	 */
	private static Interceptor getBasicAuthInterceptor(String username, String password) {
		String credentials = username + ":" + password;
		String basic = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

		return chain -> {
			Request newRequest = chain.request().newBuilder().header("Authorization", basic).build();
			return chain.proceed(newRequest);
		};
	}

}
