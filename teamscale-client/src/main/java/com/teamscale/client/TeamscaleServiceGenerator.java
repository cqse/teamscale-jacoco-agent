package com.teamscale.client;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.io.File;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/** Helper class for generating a teamscale compatible service. */
public class TeamscaleServiceGenerator {

	/**
	 * Generates a {@link Retrofit} instance for the given service, which uses basic auth to authenticate against the
	 * server and which sets the accept header to json.
	 */
	public static <S> S createService(Class<S> serviceClass, HttpUrl baseUrl, String username, String password) {
		OkHttpClient client = getDefaultOkHttpClient(username, password);
		return createService(serviceClass, baseUrl, client);
	}

	public static <S> S createServiceWithRequestLogging(Class<S> serviceClass, HttpUrl baseUrl, String username,
														String password, File file) {
		OkHttpClient client = getDefaultOkHttpClient(username, password).newBuilder()
				.addInterceptor(new FileLoggingInterceptor(file)).build();
		return createService(serviceClass, baseUrl, client);
	}

	private static <S> S createService(Class<S> serviceClass, HttpUrl baseUrl, OkHttpClient client) {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(client)
				.addConverterFactory(MoshiConverterFactory.create())
				.build();
		return retrofit.create(serviceClass);
	}

	private static OkHttpClient getDefaultOkHttpClient(String username, String password) {
		OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
		httpClient.connectTimeout(60, TimeUnit.SECONDS);
		httpClient.readTimeout(60, TimeUnit.SECONDS);
		httpClient.writeTimeout(60, TimeUnit.SECONDS);

		httpClient.addInterceptor(TeamscaleServiceGenerator.getBasicAuthInterceptor(username, password));
		httpClient.addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
				.header("Accept", "application/json").build()));

		return httpClient.build();
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
