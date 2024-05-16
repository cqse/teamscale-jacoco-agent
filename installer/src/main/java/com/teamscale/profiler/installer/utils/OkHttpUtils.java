package com.teamscale.profiler.installer.utils;

import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for creating an {@link OkHttpClient}
 */
public class OkHttpUtils {

	/**
	 * The ssl-protocol used in all clients
	 */
	private static final String PROTOCOL = "TLS";

	/**
	 * Creates the {@link OkHttpClient} based on the given connection settings.
	 */
	public static OkHttpClient createClient(boolean validateSsl, long timeoutInSeconds) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();

		setTimeouts(builder, timeoutInSeconds);
		builder.followRedirects(false).followSslRedirects(false);

		if (!validateSsl) {
			disableSslValidation(builder);
		}

		return builder.build();
	}

	private static void disableSslValidation(OkHttpClient.Builder builder) {
		SSLSocketFactory sslSocketFactory;
		try {
			SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
			sslContext.init(null, new TrustManager[]{TrustAllCertificatesManager.INSTANCE}, new SecureRandom());
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			System.err.println("Could not disable SSL certificate validation. Leaving it enabled");
			return;
		}

		builder.sslSocketFactory(sslSocketFactory, TrustAllCertificatesManager.INSTANCE);
		builder.hostnameVerifier((hostName, session) -> true);
	}

	private static void setTimeouts(OkHttpClient.Builder builder, long timeoutInSeconds) {
		builder.connectTimeout(timeoutInSeconds, TimeUnit.SECONDS);
		builder.readTimeout(timeoutInSeconds, TimeUnit.SECONDS);
		builder.writeTimeout(timeoutInSeconds, TimeUnit.SECONDS);
	}

	private static class TrustAllCertificatesManager implements X509TrustManager {
		private static final TrustAllCertificatesManager INSTANCE = new TrustAllCertificatesManager();

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			// do nothing, i.e. accept all certificates
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			// do nothing, i.e. accept all certificates
		}
	}
}
