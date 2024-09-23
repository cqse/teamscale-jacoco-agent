package com.teamscale.jacoco.agent.options;

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
}
