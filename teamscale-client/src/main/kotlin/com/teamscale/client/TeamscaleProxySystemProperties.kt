package com.teamscale.client

/**
 * Reads and writes Java system properties values for
 *
 *  * teamscale.http.proxyHost
 *  * teamscale.http.proxyPort
 *  * teamscale.http.proxyUser
 *  * teamscale.http.proxyPassword
 *
 * or the corresponding HTTPS counterpart (starting with https instead of http).
 * These values set the proxy server and credentials that should be used later to reach Teamscale and take precedence
 * over the default proxy settings (see [ProxySystemProperties.ProxySystemProperties]).
 */
class TeamscaleProxySystemProperties(protocol: Protocol) : ProxySystemProperties(protocol) {
	override val propertyPrefix: String
		get() = TEAMSCALE_PREFIX

	companion object {
		const val TEAMSCALE_PREFIX = "teamscale."
	}
}