package com.teamscale.client

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

internal class ProxySystemPropertiesTest {
	@Test
	fun testPortParsing() {
		properties.proxyPort = 9876
		assertThat(properties.proxyPort).isEqualTo(9876)
		assertThatThrownBy {
			properties.proxyPort = 0
		}.hasMessage("Port must be a positive integer")
		assertThatThrownBy {
			properties.proxyPort = 65536
		}.hasMessage("Port must be less than or equal to 65535")
		properties.clear()
		assertThat(properties.proxyPort).isEqualTo(-1)
	}

	companion object {
		private val properties = ProxySystemProperties(ProxySystemProperties.Protocol.HTTP)

		@JvmStatic
		@AfterAll
		fun teardown() {
			properties.clear()
		}
	}
}