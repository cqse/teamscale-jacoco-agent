package com.teamscale.client

import okhttp3.Authenticator
import okhttp3.Credentials.basic
import okhttp3.Interceptor
import okhttp3.OkHttpClient.Builder
import okhttp3.Response
import okhttp3.Route
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.*
import java.util.function.Consumer
import javax.net.ssl.*

/**
 * Utility functions to set up [Retrofit] and [OkHttpClient].
 */
object HttpUtils {
	private val LOGGER: Logger = LoggerFactory.getLogger(HttpUtils::class.java)

	/**
	 * Default read timeout in seconds.
	 */
	@JvmField
	val DEFAULT_READ_TIMEOUT: Duration = Duration.ofSeconds(60)

	/**
	 * Default write timeout in seconds.
	 */
	@JvmField
	val DEFAULT_WRITE_TIMEOUT: Duration = Duration.ofSeconds(60)

	/**
	 * HTTP header used for authenticating against a proxy server
	 */
	const val PROXY_AUTHORIZATION_HTTP_HEADER: String = "Proxy-Authorization"

	/** Controls whether [OkHttpClient]s built with this class will validate SSL certificates.  */
	private var shouldValidateSsl = true

	/** @see .shouldValidateSsl
	 */
	@JvmStatic
	fun setShouldValidateSsl(shouldValidateSsl: Boolean) {
		HttpUtils.shouldValidateSsl = shouldValidateSsl
	}

	/**
	 * Creates a new [Retrofit] with proper defaults. The instance and the corresponding [OkHttpClient] can
	 * be customized with the given action. Timeouts for reading and writing can be customized.
	 */
	/**
	 * Creates a new [Retrofit] with proper defaults. The instance and the corresponding [OkHttpClient] can
	 * be customized with the given action. Read and write timeouts are set according to the default values.
	 */
	@JvmOverloads
	@JvmStatic
	fun createRetrofit(
		retrofitBuilderAction: Consumer<Retrofit.Builder>,
		okHttpBuilderAction: Consumer<Builder>, readTimeout: Duration = DEFAULT_READ_TIMEOUT,
		writeTimeout: Duration = DEFAULT_WRITE_TIMEOUT
	): Retrofit {
		val httpClientBuilder = Builder()
		setTimeouts(httpClientBuilder, readTimeout, writeTimeout)
		setUpSslValidation(httpClientBuilder)
		setUpProxyServer(httpClientBuilder)
		okHttpBuilderAction.accept(httpClientBuilder)

		val builder = Retrofit.Builder().client(httpClientBuilder.build())
		retrofitBuilderAction.accept(builder)
		return builder.build()
	}

	/**
	 * Java and/or OkHttp do not pick up the http.proxy* and https.proxy* system properties reliably. We need to teach
	 * OkHttp to always pick them up.
	 *
	 *
	 * Sources: [https://memorynotfound.com/configure-http-proxy-settings-java/](https://memorynotfound.com/configure-http-proxy-settings-java/)
	 * &
	 * [https://stackoverflow.com/a/35567936](https://stackoverflow.com/a/35567936)
	 */
	private fun setUpProxyServer(httpClientBuilder: Builder) {
		val setHttpsProxyWasSuccessful = setUpProxyServerForProtocol(
			ProxySystemProperties.Protocol.HTTPS,
			httpClientBuilder
		)
		if (!setHttpsProxyWasSuccessful) {
			setUpProxyServerForProtocol(ProxySystemProperties.Protocol.HTTP, httpClientBuilder)
		}
	}

	private fun setUpProxyServerForProtocol(
		protocol: ProxySystemProperties.Protocol,
		httpClientBuilder: Builder
	): Boolean {
		val teamscaleProxySystemProperties = TeamscaleProxySystemProperties(protocol)
		try {
			if (!teamscaleProxySystemProperties.isProxyServerSet()) {
				return false
			}

			useProxyServer(
				httpClientBuilder, teamscaleProxySystemProperties.proxyHost!!,
				teamscaleProxySystemProperties.proxyPort
			)
		} catch (e: ProxySystemProperties.IncorrectPortFormatException) {
			LOGGER.warn(e.message)
			return false
		}

		if (teamscaleProxySystemProperties.isProxyAuthSet()) {
			useProxyAuthenticator(
				httpClientBuilder,
				teamscaleProxySystemProperties.proxyUser!!,
				teamscaleProxySystemProperties.proxyPassword!!
			)
		}

		return true
	}

	private fun useProxyServer(httpClientBuilder: Builder, proxyHost: String, proxyPort: Int) {
		httpClientBuilder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
	}

	private fun useProxyAuthenticator(httpClientBuilder: Builder, user: String, password: String) {
		val proxyAuthenticator = Authenticator { route: Route?, response: Response ->
			val credential = basic(user, password)
			response.request.newBuilder()
				.header(PROXY_AUTHORIZATION_HTTP_HEADER, credential)
				.build()
		}
		httpClientBuilder.proxyAuthenticator(proxyAuthenticator)
	}


	/**
	 * Sets sensible defaults for the [OkHttpClient].
	 */
	private fun setTimeouts(builder: Builder, readTimeout: Duration, writeTimeout: Duration) {
		builder.connectTimeout(Duration.ofSeconds(60))
		builder.readTimeout(readTimeout)
		builder.writeTimeout(writeTimeout)
	}

	/**
	 * Enables or disables SSL certificate validation for the [Retrofit] instance
	 */
	private fun setUpSslValidation(builder: Builder) {
		if (shouldValidateSsl) {
			// this is the default behaviour of OkHttp, so we don't need to do anything
			return
		}

		val sslSocketFactory: SSLSocketFactory
		try {
			val sslContext = SSLContext.getInstance("TLS")
			sslContext.init(null, arrayOf<TrustManager>(TrustAllCertificatesManager.INSTANCE), SecureRandom())
			sslSocketFactory = sslContext.socketFactory
		} catch (e: GeneralSecurityException) {
			LOGGER.error("Could not disable SSL certificate validation. Leaving it enabled", e)
			return
		}

		// this causes OkHttp to accept all certificates
		builder.sslSocketFactory(sslSocketFactory, TrustAllCertificatesManager.INSTANCE)
		// this causes it to ignore invalid host names in the certificates
		builder.hostnameVerifier(HostnameVerifier { hostName: String?, session: SSLSession? -> true })
	}

	/**
	 * Returns the error body of the given response or a replacement string in case it is null.
	 */
	@Throws(IOException::class)
	@JvmStatic
	fun <T> getErrorBodyStringSafe(response: retrofit2.Response<T>): String {
		val errorBody = response.errorBody() ?: return "<no response body provided>"
		return errorBody.string()
	}

	/**
	 * Returns an interceptor, which adds a basic auth header to a request.
	 */
	@JvmStatic
	fun getBasicAuthInterceptor(username: String, password: String): Interceptor {
		val credentials = "$username:$password"
		val basic = "Basic " + Base64.getEncoder().encodeToString(credentials.toByteArray())

		return Interceptor { chain: Interceptor.Chain ->
			val newRequest = chain.request().newBuilder().header("Authorization", basic).build()
			chain.proceed(newRequest)
		}
	}

	/**
	 * A simple implementation of [X509TrustManager] that simple trusts every certificate.
	 */
	class TrustAllCertificatesManager : X509TrustManager {
		/** Returns `null`.  */
		override fun getAcceptedIssuers(): Array<X509Certificate> {
			return arrayOf()
		}

		/** Does nothing.  */
		override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {
			// Nothing to do
		}

		/** Does nothing.  */
		override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
			// Nothing to do
		}

		companion object {
			/** Singleton instance.  */ /*package*/
			val INSTANCE: TrustAllCertificatesManager = TrustAllCertificatesManager()
		}
	}
}
