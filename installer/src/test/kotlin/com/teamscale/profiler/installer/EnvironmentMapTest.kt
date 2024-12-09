package com.teamscale.profiler.installer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class EnvironmentMapTest {
	@Test
	fun testQuoting() {
		Assertions.assertThat(JvmEnvironmentMap("V", "a b").systemdString).isEqualTo("\"V=a b\"")
		Assertions.assertThat(JvmEnvironmentMap("V", "a b").etcEnvironmentLinesList).containsExactlyInAnyOrder(
			"V=\"a b\""
		)
		Assertions.assertThat(JvmEnvironmentMap("V", "a b").environmentVariableMap).containsEntry("V", "\"a b\"")
	}

	@Test
	fun testMultipleEntries() {
		Assertions.assertThat(JvmEnvironmentMap("V", "a", "E", "b").systemdString).isEqualTo("E=b V=a")
		Assertions.assertThat(JvmEnvironmentMap("V", "a", "E", "b").etcEnvironmentLinesList).containsExactlyInAnyOrder(
			"E=b", "V=a"
		)
	}
}