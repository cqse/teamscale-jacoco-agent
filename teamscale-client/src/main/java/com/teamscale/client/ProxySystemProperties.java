package com.teamscale.client;

/**
 * Reads and writes Java system properties values for
 * <ul>
 *     <li>http.proxyHost</li>
 *     <li>http.proxyPort</li>
 *     <li>http.proxyUser</li>
 *     <li>http.proxyPassword</li>
 * </ul>
 * or the corresponding HTTPS counterpart (starting with https instead of http).
 * These values set the proxy server and credentials that should be used later to reach Teamscale.
 */
public class ProxySystemProperties {

	private static final String PROXY_HOST_SYSTEM_PROPERTY = ".proxyHost";
	private static final String PROXY_PORT_SYSTEM_PROPERTY = ".proxyPort";
	private static final String PROXY_USER_SYSTEM_PROPERTY = ".proxyUser";
	private static final String PROXY_PASSWORD_SYSTEM_PROPERTY = ".proxyPassword";

	private Protocol protocol;

	/**
	 * Indicates, whether the {@link ProxySystemProperties} should return values for the http.proxy* system properties
	 * or the https.proxy* ones
	 */
	public enum Protocol {
		HTTP,
		HTTPS;

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	/**
	 * @param protocol Indicates, whether the {@link ProxySystemProperties} should return values for the http.proxy*
	 *                 system properties or the https.proxy* ones
	 */
	public ProxySystemProperties(Protocol protocol) {
		this.protocol = protocol;
	}

	/**
	 * Checks whether proxyHost and proxyPort are set
	 */
	public boolean proxyServerIsSet() {
		return !StringUtils.isEmpty(getProxyHost()) && getProxyPort() > 0;
	}

	/**
	 * Checks whether proxyUser and proxyPassword are set
	 */
	public boolean proxyAuthIsSet() {
		return !StringUtils.isEmpty(getProxyUser()) && !StringUtils.isEmpty(getProxyPassword());
	}

	/**
	 * Read the http(s).proxyHost system variable
	 */
	public String getProxyHost() {
		return System.getProperty(protocol + PROXY_HOST_SYSTEM_PROPERTY);
	}

	/**
	 * Set the http(s).proxyHost system variable
	 */
	public void setProxyHost(String proxyHost) {
		System.setProperty(protocol + PROXY_HOST_SYSTEM_PROPERTY, proxyHost);
	}

	/**
	 * Read the http(s).proxyPort system variable
	 */
	public int getProxyPort() {
		return parsePort(System.getProperty(protocol + PROXY_PORT_SYSTEM_PROPERTY));
	}

	/**
	 * Set the http(s).proxyPort system variable
	 */
	public void setProxyPort(int proxyPort) {
		setProxyPort(proxyPort + "");
	}

	/**
	 * Set the http(s).proxyPort system variable
	 */
	public void setProxyPort(String proxyPort) {
		System.setProperty(protocol + PROXY_PORT_SYSTEM_PROPERTY, proxyPort);
	}

	/**
	 * Get the http(s).proxyUser system variable
	 */
	public String getProxyUser() {
		return System.getProperty(protocol + PROXY_USER_SYSTEM_PROPERTY);
	}

	/**
	 * Set the http(s).proxyUser system variable
	 */
	public void setProxyUser(String proxyUser) {
		System.setProperty(protocol + PROXY_USER_SYSTEM_PROPERTY, proxyUser);
	}

	/**
	 * Get the http(s).proxyPassword system variable
	 */
	public String getProxyPassword() {
		return System.getProperty(protocol + PROXY_PASSWORD_SYSTEM_PROPERTY);
	}

	/**
	 * Set the http(s).proxyPassword system variable
	 */
	public void setProxyPassword(String proxyPassword) {
		System.setProperty(protocol + PROXY_PASSWORD_SYSTEM_PROPERTY, proxyPassword);
	}

	private int parsePort(String portString) {
		try {
			return Integer.parseInt(portString);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
