package com.teamscale.client

import com.teamscale.client.utils.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Reads and writes Java system properties values for
 *
 *  * http.proxyHost
 *  * http.proxyPort
 *  * http.proxyUser
 *  * http.proxyPassword
 *
 * or the corresponding HTTPS counterpart (starting with https instead of http).
 * These values set the proxy server and credentials that should be used later to reach Teamscale.
 */
class ProxySystemProperties
/**
 * @param protocol Indicates, whether the [ProxySystemProperties] should use values for the http.proxy* system
 * properties or the https.proxy* ones
 */(private val protocol: Protocol) {
	/**
	 * Indicates, whether the [ProxySystemProperties] should return values for the http.proxy* system properties
	 * or the https.proxy* ones
	 */
	enum class Protocol {
		HTTP,
		HTTPS;

		override fun toString(): String {
			return name.lowercase(Locale.getDefault())
		}
	}

	/**
	 * Checks whether proxyHost and proxyPort are set
	 */
	fun proxyServerIsSet(): Boolean {
		return !StringUtils.isEmpty(proxyHost) && proxyPort > 0
	}

	/**
	 * Checks whether proxyUser and proxyPassword are set
	 */
	fun proxyAuthIsSet(): Boolean {
		return !StringUtils.isEmpty(proxyUser) && !StringUtils.isEmpty(
			proxyPassword
		)
	}

	var proxyHost: String
		/**
		 * Read the http(s).proxyHost system variable
		 */
		get() = System.getProperty(proxyHostSystemPropertyName) ?: ""
		/**
		 * Set the http(s).proxyHost system variable
		 */
		set(proxyHost) {
			System.setProperty(proxyHostSystemPropertyName, proxyHost)
		}

	var proxyPort: Int
		/**
		 * Read the http(s).proxyPort system variable.
		 * Returns -1 if no or an invalid port was set.
		 */
		get() = parsePort(System.getProperty(proxyPortSystemPropertyName))
		/**
		 * Set the http(s).proxyPort system variable
		 */
		set(proxyPort) {
			setProxyPort(proxyPort.toString() + "")
		}

	private val proxyHostSystemPropertyName: String
		get() = protocol.toString() + PROXY_HOST_SYSTEM_PROPERTY

	/**
	 * Set the http(s).proxyPort system variable
	 */
	fun setProxyPort(proxyPort: String?) {
		System.setProperty(proxyPortSystemPropertyName, proxyPort)
	}

	/**
	 * Removes the http(s).proxyPort system variable.
	 * For testing.
	 */
	/*package*/
	fun removeProxyPort() {
		System.clearProperty(proxyPortSystemPropertyName)
	}

	private val proxyPortSystemPropertyName: String
		get() = protocol.toString() + PROXY_PORT_SYSTEM_PROPERTY

	var proxyUser: String
		/**
		 * Get the http(s).proxyUser system variable
		 */
		get() = System.getProperty(proxyUserSystemPropertyName) ?: ""
		/**
		 * Set the http(s).proxyUser system variable
		 */
		set(proxyUser) {
			System.setProperty(proxyUserSystemPropertyName, proxyUser)
		}

	private val proxyUserSystemPropertyName: String
		get() = protocol.toString() + PROXY_USER_SYSTEM_PROPERTY

	var proxyPassword: String
		/**
		 * Get the http(s).proxyPassword system variable
		 */
		get() = System.getProperty(proxyPasswordSystemPropertyName) ?: ""
		/**
		 * Set the http(s).proxyPassword system variable
		 */
		set(proxyPassword) {
			System.setProperty(proxyPasswordSystemPropertyName, proxyPassword)
		}


	private val proxyPasswordSystemPropertyName: String
		get() = protocol.toString() + PROXY_PASSWORD_SYSTEM_PROPERTY

	/** Parses the given port string. Returns -1 if the string is null or not a valid number.  */
	private fun parsePort(portString: String?): Int {
		if (portString == null) {
			return -1
		}

		try {
			return portString.toInt()
		} catch (e: NumberFormatException) {
			LOGGER.warn(
				"Could not parse proxy port \"" + portString +
						"\" set via \"" + proxyPortSystemPropertyName + "\""
			)
			return -1
		}
	}

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(ProxySystemProperties::class.java)

		private const val PROXY_HOST_SYSTEM_PROPERTY = ".proxyHost"
		private const val PROXY_PORT_SYSTEM_PROPERTY = ".proxyPort"
		private const val PROXY_USER_SYSTEM_PROPERTY = ".proxyUser"
		private const val PROXY_PASSWORD_SYSTEM_PROPERTY = ".proxyPassword"
	}
}
