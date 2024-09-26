package com.teamscale.jacoco.agent.options;

import com.teamscale.client.ProxySystemProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeamscaleProxyOptionsTest {

	@Test
	void testTeamscaleProxyOptionsFilledWithJVMOptionsOnInit() {
		ProxySystemProperties proxySystemProperties =new ProxySystemProperties(ProxySystemProperties.Protocol.HTTP);
		String expectedHost = "testHost";
		proxySystemProperties.setProxyHost(expectedHost);
		int expectedPort = 1234;
		proxySystemProperties.setProxyPort(expectedPort);
		String expectedUser = "testUser";
		proxySystemProperties.setProxyUser(expectedUser);
		String expectedPassword = "testPassword";
		proxySystemProperties.setProxyPassword(expectedPassword);

		TeamscaleProxyOptions teamscaleProxyOptions = new TeamscaleProxyOptions(ProxySystemProperties.Protocol.HTTP);
		assertThat(teamscaleProxyOptions.proxyHost).isEqualTo(expectedHost);
		assertThat(teamscaleProxyOptions.proxyPort).isEqualTo(expectedPort);
		assertThat(teamscaleProxyOptions.proxyUser).isEqualTo(expectedUser);
		assertThat(teamscaleProxyOptions.proxyPassword).isEqualTo(expectedPassword);

		proxySystemProperties.setProxyHost("");
		proxySystemProperties.setProxyPort("");
		proxySystemProperties.setProxyUser("");
		proxySystemProperties.setProxyPassword("");
	}
}