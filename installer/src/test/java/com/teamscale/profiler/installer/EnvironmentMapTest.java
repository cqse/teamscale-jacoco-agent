package com.teamscale.profiler.installer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentMapTest {

	@Test
	void testEscaping() {
		assertThat(new EnvironmentMap("V", "a b").getSystemdString()).isEqualTo("\"V=a b\"");
		assertThat(new EnvironmentMap("V", "\\a").getSystemdString()).isEqualTo("\"V=\\\\a\"");
		assertThat(new EnvironmentMap("V", "\"").getSystemdString()).isEqualTo("\"V=\\\"\"");
		assertThat(new EnvironmentMap("V", "a b").getEtcEnvironmentString()).isEqualTo("V=\"a b\"");
		assertThat(new EnvironmentMap("V", "\\a").getEtcEnvironmentString()).isEqualTo("V=\"\\\\a\"");
		assertThat(new EnvironmentMap("V", "\"").getEtcEnvironmentString()).isEqualTo("V=\"\\\"\"");
	}

	@Test
	void testMultipleEntries() {
		assertThat(new EnvironmentMap("V", "a", "E", "b").getSystemdString()).isEqualTo("E=b V=a");
		assertThat(new EnvironmentMap("V", "a", "E", "b").getEtcEnvironmentString()).isEqualTo("E=b\nV=a");
	}

}