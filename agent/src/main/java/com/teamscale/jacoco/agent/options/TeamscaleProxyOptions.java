package com.teamscale.jacoco.agent.options;

import com.teamscale.client.ProxySystemProperties;
import com.teamscale.client.TeamscaleProxySystemProperties;

import java.nio.file.Path;

/**
 * Parses agent command line options related to the proxy settings.
 */
public class TeamscaleProxyOptions {

	/** The host of the proxy server.  */
	/* package */ String proxyHost;

	/** The port of the proxy server. */
	/* package */ int proxyPort;

	/** The password for the proxy user. */
	/* package */ String proxyPassword;

	/** A path to the file that contains the password for the proxy authentication. */
	/* package */ Path proxyPasswordPath;

	/** The username of the proxy user. */
	/* package */ String proxyUser;


	/**
	 * Handles all command-line options prefixed with 'proxy-'
	 *
	 * @return true if it has successfully processed the given option.
	 */
	public static boolean handleTeamscaleProxyOptions(TeamscaleProxyOptions options, String key, String value, FilePatternResolver filePatternResolver) throws AgentOptionParseException {
		switch (key) {
			case "proxy-password-file":
				options.proxyPasswordPath = filePatternResolver.parsePath(key, value);
				return true;
			case "proxy-host":
				options.proxyHost = value;
				return true;
			case "proxy-port":
				options.proxyPort = Integer.parseInt(value);
				return true;
			case "proxy-user":
				options.proxyUser = value;
				return true;
			case "proxy-password":
				options.proxyPassword = value;
				return true;
			default:
				return false;
		}
	}

	/** Stores the teamscale-specific proxy settings as system properties to make them always available. */
	public void putTeamscaleProxyOptionsIntoSystemProperties() {
		putTeamscaleProxyOptionsIntoSystemPropertiesForProtocol(ProxySystemProperties.Protocol.HTTP);
		putTeamscaleProxyOptionsIntoSystemPropertiesForProtocol(ProxySystemProperties.Protocol.HTTPS);
	}

	private void putTeamscaleProxyOptionsIntoSystemPropertiesForProtocol(ProxySystemProperties.Protocol protocol) {
		if (proxyHost != null) {
			new TeamscaleProxySystemProperties(protocol).setProxyHost(proxyHost);
		}
		if (proxyPort > 0) {
			new TeamscaleProxySystemProperties(protocol).setProxyPort(proxyPort);
		}
		if(proxyUser != null) {
			new TeamscaleProxySystemProperties(protocol).setProxyUser(proxyUser);
		}
		if(proxyPassword != null) {
			new TeamscaleProxySystemProperties(protocol).setProxyPassword(proxyPassword);
		}
	}
}
