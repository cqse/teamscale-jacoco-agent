package com.teamscale.profiler.installer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentMapTest {

	@Test
	void testQuoting() {
		assertThat(new JvmEnvironmentMap("V", "a b").getSystemdString()).isEqualTo("\"V=a b\"");
		assertThat(new JvmEnvironmentMap("V", "a b").getEtcEnvironmentLinesList()).containsExactlyInAnyOrder(
				"V=\"a b\"");
		assertThat(new JvmEnvironmentMap("V", "a b").getEnvironmentVariableMap()).containsEntry("V", "\"a b\"");
	}

	@Test
	void testMultipleEntries() {
		assertThat(new JvmEnvironmentMap("V", "a", "E", "b").getSystemdString()).isEqualTo("E=b V=a");
		assertThat(new JvmEnvironmentMap("V", "a", "E", "b").getEtcEnvironmentLinesList()).containsExactlyInAnyOrder(
				"E=b", "V=a");
	}

}