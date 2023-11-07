package com.teamscale.profiler.installer;

import okhttp3.OkHttpClient;
import org.jetbrains.nativecerts.NativeTrustedCertificates;
import org.jetbrains.nativecerts.NativeTrustedRootsInternalUtils;
import org.jetbrains.nativecerts.linux.LinuxTrustedCertificatesUtil;
import org.jetbrains.nativecerts.mac.SecurityFramework;
import org.jetbrains.nativecerts.mac.SecurityFrameworkUtil;
import org.jetbrains.nativecerts.win32.Crypt32ExtUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
	 *
	 * @param trustStorePath
	 *            May be null if no trust store should be used.
	 * @param trustStorePassword
	 *            May be null if no trust store should be used.
	 */
	public static OkHttpClient createClient(boolean validateSsl, String trustStorePath, String trustStorePassword,
											long timeoutInSeconds) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();

		setTimeouts(builder, timeoutInSeconds);
		builder.followRedirects(false).followSslRedirects(false);

		configureTrustStore(builder, trustStorePath, trustStorePassword);
		if (!validateSsl) {
			disableSslValidation(builder);
		}

		return builder.build();
	}

	/**
	 * Reads the keystore at the given path and configures the builder so the
	 * {@link OkHttpClient} will accept the certificates stored in the keystore.
	 */
	private static void configureTrustStore(OkHttpClient.Builder builder, String trustStorePath,
											String trustStorePassword) {

		try {
			SSLContext sslContext = SSLContext.getInstance(PROTOCOL);

			List<TrustManager> trustManagers = new ArrayList<>();
			trustManagers.addAll(getJVMTrustManagers());
			trustManagers.addAll(getOSTrustManagers());
			trustManagers.addAll(getExternalTrustManagers(trustStorePath, trustStorePassword));

			MultiTrustManager multiTrustManager = new MultiTrustManager(trustManagers);

			sslContext.init(null, new TrustManager[]{multiTrustManager}, new SecureRandom());
			builder.sslSocketFactory(sslContext.getSocketFactory(), multiTrustManager);
		} catch (NoSuchAlgorithmException e) {
			LogUtils.failWithStackTrace(e, "Failed to instantiate an SSLContext or TrustManagerFactory.");
		} catch (KeyManagementException e) {
			LogUtils.failWithStackTrace(e, "Failed to initialize the SSLContext with the trust managers.");
		} catch (ClassCastException e) {
			LogUtils.failWithStackTrace(
					e, "Trust manager is not of X509 format.");
		}
	}

	private static List<TrustManager> getExternalTrustManagers(String keystorePath, String keystorePassword) {
		if (keystorePath == null) {
			return Collections.emptyList();
		}

		try (FileInputStream stream = new FileInputStream(keystorePath)) {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(stream, keystorePassword.toCharArray());

			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);

			List<TrustManager> trustManagers = Arrays.asList(trustManagerFactory.getTrustManagers());

			if (trustManagers.isEmpty()) {
				LogUtils.fail("No custom trust managers found. This is a bug. Please report it to CQSE (support@teamscale.com).");
			}

			return trustManagers;

		} catch (NoSuchAlgorithmException e) {
			LogUtils.failWithStackTrace(e, "Failed to instantiate an SSLContext or TrustManagerFactory.");
		} catch (IOException e) {
			LogUtils.failWithoutStackTrace("Failed to read keystore file " + keystorePath
					+ "\nPlease make sure that file exists and is readable and that you provided the correct password."
					+ " Please also make sure that the keystore file is a valid Java keystore."
					+ " You can use the program `keytool` from your JVM installation to check this:"
					+ "\nkeytool -list -keystore " + keystorePath, e);
		} catch (KeyStoreException e) {
			LogUtils.failWithStackTrace(e, "Failed to initialize the TrustManagerFactory with the keystore.");
		} catch (CertificateException e) {
			LogUtils.failWithoutStackTrace("Failed to load one of the certificates in the keystore file " + keystorePath
							+ "\nPlease make sure that the certificate is stored correctly and the certificate version and encoding are supported.",
					e);
		}
		return Collections.emptyList();
	}

	/**
	 * Returns the {@link TrustManager trust managers} of the JVM.
	 */
	private static List<TrustManager> getJVMTrustManagers() {
		try {
			TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init((KeyStore) null);

			return Arrays.asList(factory.getTrustManagers());
		} catch (KeyStoreException | NoSuchAlgorithmException e) {
			LogUtils.warn("Could not import certificates from the JVM.", e);
			return Collections.emptyList();
		}

	}

	/**
	 * Returns the {@link TrustManager trust managers} of the OS.
	 */
	private static List<TrustManager> getOSTrustManagers() {
		try {
			Collection<X509Certificate> osCertificates = getCustomOsTrustedCertificates();

			if (osCertificates.isEmpty()) {
				LogUtils.info("Imported 0 certificates from the operating system.");
				return Collections.emptyList();
			}

			// Create an empty keystore
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null);
			for (X509Certificate certificate : osCertificates) {
				keyStore.setCertificateEntry(certificate.getSubjectX500Principal().getName(), certificate);
			}

			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);

			LogUtils.info(String.format("Imported %s certificates from the operating system.", osCertificates.size()));

			for (X509Certificate certificate : osCertificates) {
				LogUtils.debug(String.format("Imported %s", certificate.getSubjectX500Principal().getName()));
			}

			return Arrays.asList(trustManagerFactory.getTrustManagers());

		} catch (Exception e) {
			// There could be reflection calls which we are missing in GraalVM
			// Make sure the program will still run
			LogUtils.warn("Could not import certificates from the operating system." +
					"\nThis is a bug. Please report it to CQSE (support@teamscale.com).", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Recreation of {@link NativeTrustedCertificates#getCustomOsSpecificTrustedCertificates()} without the error handling.
	 * This enables us to use our own logging and exception handling.
	 */
	private static Collection<X509Certificate> getCustomOsTrustedCertificates() {
		if (NativeTrustedRootsInternalUtils.isLinux) {
			return LinuxTrustedCertificatesUtil.getSystemCertificates();
		} else if (NativeTrustedRootsInternalUtils.isMac) {
			List<X509Certificate> admin = SecurityFrameworkUtil.getTrustedRoots(SecurityFramework.SecTrustSettingsDomain.admin);
			List<X509Certificate> user = SecurityFrameworkUtil.getTrustedRoots(SecurityFramework.SecTrustSettingsDomain.user);
			Set<X509Certificate> result = new HashSet<>(admin);
			result.addAll(user);
			return result;
		} else if (NativeTrustedRootsInternalUtils.isWindows) {
			return Crypt32ExtUtil.getCustomTrustedRootCertificates();
		} else {
			LogUtils.warn("Could not import certificates from the operating system: unsupported system, not a Linux/Mac OS/Windows: " + System.getProperty("os.name"));
			return Collections.emptySet();
		}
	}

	private static void disableSslValidation(OkHttpClient.Builder builder) {
		SSLSocketFactory sslSocketFactory;
		try {
			SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
			sslContext.init(null, new TrustManager[] { TrustAllCertificatesManager.INSTANCE }, new SecureRandom());
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (GeneralSecurityException e) {
			LogUtils.warn("Could not disable SSL certificate validation. Leaving it enabled", e);
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
		static final TrustAllCertificatesManager INSTANCE = new TrustAllCertificatesManager();

		public TrustAllCertificatesManager() {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}
	}

	/**
	 * Combines multiple {@link X509TrustManager}.
	 * If one of the managers trust the certificate chain, the {@link MultiTrustManager} will trust the certificate.
	 */
	private static class MultiTrustManager implements X509TrustManager {

		private final List<X509TrustManager> trustManagers;

		private MultiTrustManager(List<TrustManager> managers) {
			trustManagers = managers.stream().map(X509TrustManager.class::cast).collect(Collectors.toList());
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return trustManagers.stream().flatMap(manager -> Arrays.stream(manager.getAcceptedIssuers())).toArray(X509Certificate[]::new);
		}


		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			checkAll(manager -> manager.checkClientTrusted(chain, authType));
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			checkAll(manager -> manager.checkServerTrusted(chain, authType));
		}

		private void checkAll(ConsumerWithException<X509TrustManager, CertificateException> check) throws CertificateException {
			Collection<CertificateException> exceptions = new ArrayList<>();

			for (X509TrustManager trustManager : trustManagers) {
				try {
					check.accept(trustManager);
					// We have found one manager which trusts the certificate
					return;
				} catch (CertificateException e) {
					exceptions.add(e);
				}
			}
			CertificateException certificateException = new CertificateException();
			exceptions.forEach(certificateException::addSuppressed);
			throw certificateException;
		}
	}

	/**
	 * Consumer which can throw an exception
	 *
	 * @see Consumer
	 */
	private interface ConsumerWithException<T, E extends Exception> {

		/**
		 * @see Consumer#accept(Object)
		 */
		void accept(T t) throws E;
	}
}
