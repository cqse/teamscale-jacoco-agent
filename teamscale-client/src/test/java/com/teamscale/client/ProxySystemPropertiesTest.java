package com.teamscale.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProxySystemPropertiesTest {

	private static ProxySystemProperties properties = new ProxySystemProperties(ProxySystemProperties.Protocol.HTTP);

	@AfterAll
	static void teardown() {
		properties.removeProxyPort();
	}

	@Test
	void testPortParsing() {
		properties.setProxyPort(9876);
		assertThat(properties.getProxyPort()).isEqualTo(9876);
		properties.setProxyPort("");
		assertThat(properties.getProxyPort()).isEqualTo(-1);
		String incorrectFormatValue = "nonsense";
		properties.setProxyPort(incorrectFormatValue);
		ProxySystemProperties.IncorrectPortFormatException exception = assertThrows(ProxySystemProperties.IncorrectPortFormatException.class,
				properties::getProxyPort);
		assertThat(exception.getMessage()).isEqualTo(String.format("Could not parse proxy port \"%s\" set via \"%s\"", incorrectFormatValue, properties.getProxyPortSystemPropertyName()));
		properties.removeProxyPort();
		assertThat(properties.getProxyPort()).isEqualTo(-1);
	}

}