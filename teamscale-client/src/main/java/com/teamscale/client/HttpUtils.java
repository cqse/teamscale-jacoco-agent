package com.teamscale.client;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Utility functions to set up {@link Retrofit} and {@link OkHttpClient}.
 */
public class HttpUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
	public static final int DEFAULT_READ_TIMEOUT = 60;
	public static final int DEFAULT_WRITE_TIMEOUT = 60;

	/** Controls whether {@link OkHttpClient}s built with this class will validate SSL certificates. */
	private static boolean shouldValidateSsl = true;

	/** @see #shouldValidateSsl */
	public static void setShouldValidateSsl(boolean shouldValidateSsl) {
		HttpUtils.shouldValidateSsl = shouldValidateSsl;
	}

	public static Retrofit createRetrofit(Consumer<Retrofit.Builder> retrofitBuilderAction,
										  Consumer<OkHttpClient.Builder> okHttpBuilderAction) {
		return createRetrofit(retrofitBuilderAction, okHttpBuilderAction, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT);
	}

	/**
	 * Creates a new {@link Retrofit} with proper defaults. The instance and the corresponding {@link OkHttpClient} can
	 * be customized with the given action.
	 */
	public static Retrofit createRetrofit(Consumer<Retrofit.Builder> retrofitBuilderAction,
										  Consumer<OkHttpClient.Builder> okHttpBuilderAction, int readTimeout, int writeTimeout) {
		OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
		setTimeouts(httpClientBuilder, readTimeout, writeTimeout);
		setUpSslValidation(httpClientBuilder);
		okHttpBuilderAction.accept(httpClientBuilder);

		Retrofit.Builder builder = new Retrofit.Builder().client(httpClientBuilder.build());
		retrofitBuilderAction.accept(builder);
		return builder.build();
	}

	/**
	 * Sets sensible defaults for the {@link OkHttpClient}.
	 */
	private static void setTimeouts(OkHttpClient.Builder builder, int readTimeout, int writeTimeout) {
		builder.connectTimeout(60, TimeUnit.SECONDS);
		builder.readTimeout(readTimeout, TimeUnit.SECONDS);
		builder.writeTimeout(writeTimeout, TimeUnit.SECONDS);
	}

	/**
	 * Enables or disables SSL certificate validation for the {@link Retrofit} instance
	 */
	private static void setUpSslValidation(OkHttpClient.Builder builder) {
		if (shouldValidateSsl) {
			// this is the default behaviour of OkHttp, so we don't need to do anything
			return;
		}

		SSLSocketFactory sslSocketFactory;
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[]{TrustAllCertificatesManager.INSTANCE}, new SecureRandom());
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (GeneralSecurityException e) {
			LOGGER.error("Could not disable SSL certificate validation. Leaving it enabled", e);
			return;
		}

		// this causes OkHttp to accept all certificates
		builder.sslSocketFactory(sslSocketFactory, TrustAllCertificatesManager.INSTANCE);
		// this causes it to ignore invalid host names in the certificates
		builder.hostnameVerifier((String hostName, SSLSession session) -> true);
	}

	/**
	 * A simple implementation of {@link X509TrustManager} that simple trusts every certificate.
	 */
	public static class TrustAllCertificatesManager implements X509TrustManager {

		/** Singleton instance. */
		/*package*/ static final TrustAllCertificatesManager INSTANCE = new TrustAllCertificatesManager();

		/** Returns <code>null</code>. */
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		/** Does nothing. */
		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			// Nothing to do
		}

		/** Does nothing. */
		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			// Nothing to do
		}

	}

	/**
	 * Returns the error body of the given response or a replacement string in case it is null.
	 */
	public static <T> String getErrorBodyStringSafe(Response<T> response) throws IOException {
		ResponseBody errorBody = response.errorBody();
		if (errorBody == null) {
			return "<no response body provided>";
		}
		return errorBody.string();
	}

	/**
	 * Returns an interceptor, which adds a basic auth header to a request.
	 */
	public static Interceptor getBasicAuthInterceptor(String username, String password) {
		String credentials = username + ":" + password;
		String basic = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

		return chain -> {
			Request newRequest = chain.request().newBuilder().header("Authorization", basic).build();
			return chain.proceed(newRequest);
		};
	}

}
