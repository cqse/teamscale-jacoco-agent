package com.teamscale.client

/**
 * Manages Java system properties for:
 * - http.proxyHost
 * - http.proxyPort
 * - http.proxyUser
 * - http.proxyPassword
 *
 * Or their HTTPS counterparts (https.*).
 * These values set the proxy server and credentials used to reach Teamscale.
 */
open class ProxySystemProperties(private val protocol: Protocol) {

	companion object {
		private const val PROXY_HOST_SYSTEM_PROPERTY = ".proxyHost"
		private const val PROXY_PORT_SYSTEM_PROPERTY = ".proxyPort"
		private const val PROXY_USER_SYSTEM_PROPERTY = ".proxyUser"
		private const val PROXY_PASSWORD_SYSTEM_PROPERTY = ".proxyPassword"
	}

	enum class Protocol {
		HTTP, HTTPS;

		override fun toString() = name.lowercase()
	}

	protected open val propertyPrefix = ""

	var proxyHost: String?
		get() = getProperty(PROXY_HOST_SYSTEM_PROPERTY)
		set(value) {
			setProperty(PROXY_HOST_SYSTEM_PROPERTY, value)
		}

	var proxyPort: Int
		get() = getProperty(PROXY_PORT_SYSTEM_PROPERTY)?.toIntOrNull() ?: -1
		set(value) {
			check(value > 0) { "Port must be a positive integer" }
			check(value <= 65535) { "Port must be less than or equal to 65535" }
			setProperty(PROXY_PORT_SYSTEM_PROPERTY, value.toString())
		}

	var proxyUser: String?
		get() = getProperty(PROXY_USER_SYSTEM_PROPERTY)
		set(value) {
			setProperty(PROXY_USER_SYSTEM_PROPERTY, value)
		}

	var proxyPassword: String?
		get() = getProperty(PROXY_PASSWORD_SYSTEM_PROPERTY)
		set(value) {
			setProperty(PROXY_PASSWORD_SYSTEM_PROPERTY, value)
		}

	private fun getProperty(property: String) =
		System.getProperty("$propertyPrefix${protocol}.$property")

	private fun setProperty(property: String, value: String?) {
		value?.let {
			check(it.isNotBlank()) { "Value must not be blank" }
			System.setProperty("$propertyPrefix${protocol}.$property", it)
		}
	}

	fun isProxyServerSet() = !proxyHost.isNullOrEmpty() && proxyPort > 0

	fun isProxyAuthSet() = !proxyUser.isNullOrEmpty() && !proxyPassword.isNullOrEmpty()

	fun clear() {
		System.clearProperty("$propertyPrefix${protocol}.$PROXY_HOST_SYSTEM_PROPERTY")
		System.clearProperty("$propertyPrefix${protocol}.$PROXY_PORT_SYSTEM_PROPERTY")
		System.clearProperty("$propertyPrefix${protocol}.$PROXY_USER_SYSTEM_PROPERTY")
		System.clearProperty("$propertyPrefix${protocol}.$PROXY_PASSWORD_SYSTEM_PROPERTY")
	}

	class IncorrectPortFormatException(message: String, cause: Throwable) : IllegalArgumentException(message, cause)
}
