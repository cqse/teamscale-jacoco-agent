package eu.cqse.teamscale.client;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Utility functions to set up {@link Retrofit} and {@link OkHttpClient}.
 */
public class HttpUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

	/** Controls whether {@link OkHttpClient}s built with this class will validate SSL certificates. */
	private static boolean shouldValidateSsl = false;

	/** @see #shouldValidateSsl */
	public static void setShouldValidateSsl(boolean shouldValidateSsl) {
		HttpUtils.shouldValidateSsl = shouldValidateSsl;
	}

	/** Creates a new {@link Retrofit} with proper defaults. The instance can be customized with the given action. */
	public static Retrofit createRetrofit(Consumer<Retrofit.Builder> retrofitBuilderAction) {
		return createRetrofit(retrofitBuilderAction, okHttpBuilder -> {
			// nothing to do
		});
	}

	/**
	 * Creates a new {@link Retrofit} with proper defaults. The instance and the corresponding {@link OkHttpClient}
	 * can be customized with the given action.
	 */
	public static Retrofit createRetrofit(Consumer<Retrofit.Builder> retrofitBuilderAction,
										  Consumer<OkHttpClient.Builder> okHttpBuilderAction) {
		OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
		setDefaults(httpClientBuilder);
		setUpSslValidation(httpClientBuilder);
		okHttpBuilderAction.accept(httpClientBuilder);

		Retrofit.Builder builder = new Retrofit.Builder().client(httpClientBuilder.build());
		retrofitBuilderAction.accept(builder);
		return builder.build();
	}

	/**
	 * Sets sensible defaults for the {@link OkHttpClient}.
	 */
	private static void setDefaults(OkHttpClient.Builder builder) {
		builder.connectTimeout(20, TimeUnit.SECONDS);
		builder.readTimeout(20, TimeUnit.SECONDS);
		builder.writeTimeout(20, TimeUnit.SECONDS);
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
	 * A simple implementation of {@link X509TrustManager} that simple trusts every
	 * certificate.
	 */
	public static class TrustAllCertificatesManager implements X509TrustManager {

		/** Singleton instance. */
		public static final TrustAllCertificatesManager INSTANCE = new TrustAllCertificatesManager();

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


}
