package com.teamscale.client;

import org.jetbrains.annotations.NotNull;

/**
 * Reads and writes Java system properties values for
 * <ul>
 *     <li>teamscale.http.proxyHost</li>
 *     <li>teamscale.http.proxyPort</li>
 *     <li>teamscale.http.proxyUser</li>
 *     <li>teamscale.http.proxyPassword</li>
 * </ul>
 * or the corresponding HTTPS counterpart (starting with https instead of http).
 * These values set the proxy server and credentials that should be used later to reach Teamscale and take precedence
 * over the default proxy settings (see {@link ProxySystemProperties}).
 */
public class TeamscaleProxySystemProperties extends ProxySystemProperties {

	private static final String TEAMSCALE_PREFIX = "teamscale.";

	/** @see ProxySystemProperties#ProxySystemProperties */
	public TeamscaleProxySystemProperties(Protocol protocol) {
		super(protocol);
	}

	@Override
	@NotNull
	protected String getPropertyPrefix() {
		return TEAMSCALE_PREFIX;
	}
}