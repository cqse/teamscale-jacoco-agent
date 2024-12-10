package com.teamscale.profiler.installer.utils

import okhttp3.OkHttpClient
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Utilities for creating an [OkHttpClient]
 */
object OkHttpUtils {
	/**
	 * The ssl-protocol used in all clients
	 */
	private const val PROTOCOL = "TLS"

	/**
	 * Creates the [OkHttpClient] based on the given connection settings.
	 */
	fun createClient(validateSsl: Boolean, timeoutInSeconds: Long): OkHttpClient {
		val builder = OkHttpClient.Builder()

		setTimeouts(builder, timeoutInSeconds)
		builder.followRedirects(false).followSslRedirects(false)

		if (!validateSsl) {
			disableSslValidation(builder)
		}

		return builder.build()
	}

	private fun disableSslValidation(builder: OkHttpClient.Builder) {
		val sslSocketFactory: SSLSocketFactory
		try {
			val sslContext = SSLContext.getInstance(PROTOCOL)
			sslContext.init(null, arrayOf<TrustManager>(TrustAllCertificatesManager), SecureRandom())
			sslSocketFactory = sslContext.socketFactory
		} catch (e: GeneralSecurityException) {
			e.printStackTrace()
			System.err.println("Could not disable SSL certificate validation. Leaving it enabled")
			return
		}

		builder.sslSocketFactory(sslSocketFactory, TrustAllCertificatesManager)
		builder.hostnameVerifier { _, _ -> true }
	}

	private fun setTimeouts(builder: OkHttpClient.Builder, timeoutInSeconds: Long) {
		builder.connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
		builder.readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
		builder.writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
	}

	private object TrustAllCertificatesManager : X509TrustManager {
		override fun getAcceptedIssuers() = arrayOf<X509Certificate>()

		override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {
			// do nothing, i.e. accept all certificates
		}

		override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
			// do nothing, i.e. accept all certificates
		}
	}
}
