package com.teamscale.client;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class ProxySystemPropertiesTest {

	private static ProxySystemProperties properties = new ProxySystemProperties(ProxySystemProperties.Protocol.HTTP);

	@AfterAll
	static void teardown() {
		properties.removeProxyPort();
	}

	@Test
	void testPortParsing() {
		properties.setProxyPort(9876);
		Assertions.assertThat(properties.getProxyPort()).isEqualTo(9876);
		properties.setProxyPort("");
		Assertions.assertThat(properties.getProxyPort()).isEqualTo(-1);
		properties.setProxyPort("nonsense");
		Assertions.assertThat(properties.getProxyPort()).isEqualTo(-1);
		properties.removeProxyPort();
		Assertions.assertThat(properties.getProxyPort()).isEqualTo(-1);
	}

}