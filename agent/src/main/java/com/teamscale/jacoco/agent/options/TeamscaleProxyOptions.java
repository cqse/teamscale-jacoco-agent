package com.teamscale.jacoco.agent.options;

import com.teamscale.client.ProxySystemProperties;
import com.teamscale.client.TeamscaleProxySystemProperties;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.util.ILogger;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Parses agent command line options related to the proxy settings.
 */
public class TeamscaleProxyOptions {

	private final ILogger logger;

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

	private final ProxySystemProperties.Protocol protocol;

	/** Constructor. */
	public TeamscaleProxyOptions(ProxySystemProperties.Protocol protocol, ILogger logger) {
		this.protocol = protocol;
		this.logger = logger;
		ProxySystemProperties proxySystemProperties = new ProxySystemProperties(protocol);
		proxyHost = proxySystemProperties.getProxyHost();
		proxyPort = proxySystemProperties.getProxyPort();
		proxyUser = proxySystemProperties.getProxyUser();
		proxyPassword = proxySystemProperties.getProxyPassword();
	}

	/**
	 * Handles all command-line options prefixed with 'proxy-'
	 *
	 * @return true if it has successfully processed the given option.
	 */
	public boolean handleTeamscaleProxyOptions(String key, String value) {
			if (String.format("proxy-%s-host", protocol).equals(key)){
				proxyHost = value;
				return true;
			}
			if (String.format("proxy-%s-port", protocol).equals(key)) {
				proxyPort = Integer.parseInt(value);
				return true;
			}
			if (String.format("proxy-%s-user", protocol).equals(key)) {
				proxyUser = value;
				return true;
			}
			if (String.format("proxy-%s-password", protocol).equals(key)) {
				proxyPassword = value;
				return true;
			}
			return false;
	}

	/** Stores the teamscale-specific proxy settings as system properties to make them always available. */
	public void putTeamscaleProxyOptionsIntoSystemProperties() {
		TeamscaleProxySystemProperties teamscaleProxySystemProperties = new TeamscaleProxySystemProperties(protocol);
		if (proxyHost != null && !proxyHost.isEmpty()) {
			teamscaleProxySystemProperties.setProxyHost(proxyHost);
		}
		if (proxyPort > 0) {
			teamscaleProxySystemProperties.setProxyPort(proxyPort);
		}
		if(proxyUser != null && !proxyUser.isEmpty()) {
			teamscaleProxySystemProperties.setProxyUser(proxyUser);
		}
		if(proxyPassword != null && !proxyPassword.isEmpty()) {
			teamscaleProxySystemProperties.setProxyPassword(proxyPassword);
		}

		setProxyPasswordFromFile(proxyPasswordPath);
	}

	/** Sets the proxy password JVM property from a file for the protocol in this instance of {@link TeamscaleProxyOptions}. */
	private void setProxyPasswordFromFile(Path proxyPasswordFilePath) {
		if (proxyPasswordFilePath == null) {
			return;
		}
		try {
			String proxyPassword = FileSystemUtils.readFileUTF8(proxyPasswordFilePath.toFile()).trim();
			new TeamscaleProxySystemProperties(protocol).setProxyPassword(proxyPassword);
		} catch (IOException e) {
			logger.error(
					"Unable to open file containing proxy password. Please make sure the file exists and the user has the permissions to read the file.",
					e);
		}
	}
}
