package com.teamscale.client;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/** Helper class for generating a teamscale compatible service. */
public class TeamscaleServiceGenerator {

	/**
	 * Generates a {@link Retrofit} instance for the given
	 * service, which uses basic auth to authenticate against the server and which sets the accept header to json.
	 */
	public static <S> S createService(Class<S> serviceClass, HttpUrl baseUrl, String username, String password) {
		OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
		httpClient.connectTimeout(60, TimeUnit.SECONDS);
		httpClient.readTimeout(60, TimeUnit.SECONDS);
		httpClient.writeTimeout(60, TimeUnit.SECONDS);

		httpClient.addInterceptor(TeamscaleServiceGenerator.getBasicAuthInterceptor(username, password));
		httpClient.addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
				.header("Accept", "application/json").build()));

		OkHttpClient client = httpClient.build();
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(client)
				.addConverterFactory(GsonConverterFactory.create())
				.build();
		return retrofit.create(serviceClass);
	}

	/**
	 * Returns an interceptor, which adds a basic auth header to a request.
	 */
	private static Interceptor getBasicAuthInterceptor(String username, String password) {
		String credentials = username + ":" + password;
		String basic = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

		return chain -> {
			Request original = chain.request();
			Request request = original.newBuilder()
					.header("Authorization", basic).build();
			return chain.proceed(request);
		};
	}
}
