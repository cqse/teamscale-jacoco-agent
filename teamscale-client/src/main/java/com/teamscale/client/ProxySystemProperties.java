package com.teamscale.client;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxySystemProperties.class);

	private static final String PROXY_HOST_SYSTEM_PROPERTY = ".proxyHost";
	private static final String PROXY_PORT_SYSTEM_PROPERTY = ".proxyPort";
	private static final String PROXY_USER_SYSTEM_PROPERTY = ".proxyUser";
	private static final String PROXY_PASSWORD_SYSTEM_PROPERTY = ".proxyPassword";

	private final Protocol protocol;

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
	 * @return a prefix to the system properties. Used in {@link TeamscaleProxySystemProperties} to differentiate them
	 * from the JVM system properties for proxies.
	 * */
	@NotNull
	protected String getPropertyPrefix() {
		return "";
	}

	/**
	 * @param protocol Indicates, whether the {@link ProxySystemProperties} should use values for the http.proxy* system
	 *                 properties or the https.proxy* ones
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

	/** Checks whether proxyUser and proxyPassword are set */
	public boolean proxyAuthIsSet() {
		return !StringUtils.isEmpty(getProxyUser()) && !StringUtils.isEmpty(getProxyPassword());
	}

	/** Read the http(s).proxyHost system variable */
	public String getProxyHost() {
		return System.getProperty(getProxyHostSystemPropertyName());
	}

	/** Read the http(s).proxyPort system variable. Returns -1 if no or an invalid port was set. */
	public int getProxyPort() {
		return parsePort(System.getProperty(getProxyPortSystemPropertyName()));
	}

	/** Set the http(s).proxyHost system variable. */
	public void setProxyHost(String proxyHost) {
		System.setProperty(getProxyHostSystemPropertyName(), proxyHost);
	}

	/** @return the name of the system property specifying the proxy host. */
	@NotNull
	protected String getProxyHostSystemPropertyName() {
		return getPropertyPrefix() + protocol + PROXY_HOST_SYSTEM_PROPERTY;
	}

	/** Set the http(s).proxyPort system variable. */
	public void setProxyPort(int proxyPort) {
		setProxyPort(proxyPort + "");
	}

	/** Set the http(s).proxyPort system variable. */
	public void setProxyPort(String proxyPort) {
		System.setProperty(getProxyPortSystemPropertyName(), proxyPort);
	}

	/** Removes the http(s).proxyPort system variable. For testing. */
	/*package*/ void removeProxyPort() {
		System.clearProperty(getProxyPortSystemPropertyName());
	}

	/** @return the name of the system property specifying the proxy port. */
	@NotNull
	protected String getProxyPortSystemPropertyName() {
		return getPropertyPrefix() + protocol + PROXY_PORT_SYSTEM_PROPERTY;
	}

	/** Get the http(s).proxyUser system variable. */
	public String getProxyUser() {
		return System.getProperty(getProxyUserSystemPropertyName());
	}

	/** Set the http(s).proxyUser system variable. */
	public void setProxyUser(String proxyUser) {
		System.setProperty(getProxyUserSystemPropertyName(), proxyUser);
	}

	/** @return the name of the system property specifying the proxy user. */
	@NotNull
	protected String getProxyUserSystemPropertyName() {
		return getPropertyPrefix() + protocol + PROXY_USER_SYSTEM_PROPERTY;
	}

	/** Get the http(s).proxyPassword system variable. */
	public String getProxyPassword() {
		return System.getProperty(getProxyPasswordSystemPropertyName());
	}


	/** Set the http(s).proxyPassword system variable. */
	public void setProxyPassword(String proxyPassword) {
		System.setProperty(getProxyPasswordSystemPropertyName(), proxyPassword);
	}

	/** @return the name of the system property specifying the proxy password. */
	@NotNull
	protected String getProxyPasswordSystemPropertyName() {
		return getPropertyPrefix() + protocol + PROXY_PASSWORD_SYSTEM_PROPERTY;
	}

	/** Parses the given port string. Returns -1 if the string is null or not a valid number. */
	private int parsePort(String portString) {
		if (portString == null) {
			return -1;
		}

		try {
			return Integer.parseInt(portString);
		} catch (NumberFormatException e) {
			LOGGER.warn("Could not parse proxy port \"" + portString +
					"\" set via \"" + getProxyPortSystemPropertyName() + "\"");
			return -1;
		}
	}
}
