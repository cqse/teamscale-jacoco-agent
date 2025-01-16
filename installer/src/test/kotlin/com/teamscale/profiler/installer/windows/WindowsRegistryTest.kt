package com.teamscale.profiler.installer.windows

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

/**
 * Tests our registry access class.
 */
@EnabledOnOs(OS.WINDOWS)
internal class WindowsRegistryTest {
	@BeforeEach
	@Throws(Exception::class)
	fun clearRegistry() {
		Runtime.getRuntime()
			.exec(arrayOf("reg", "delete", "HKLM\\${WindowsRegistry.ENVIRONMENT_REGISTRY_KEY}", "/v", VARIABLE))
	}

	@Test
	@Throws(Exception::class)
	fun testAllFunctions() {
		Assertions.assertThat(WindowsRegistry.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE))
			.isNull()

		WindowsRegistry.setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE, "foobar")
		Assertions.assertThat(WindowsRegistry.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE))
			.isEqualTo("foobar")

		WindowsRegistry.setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE, "goo")
		Assertions.assertThat(WindowsRegistry.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE))
			.isEqualTo("goo")

		WindowsRegistry.deleteHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE)
		Assertions.assertThat(WindowsRegistry.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE))
			.isNull()
	}

	companion object {
		private const val VARIABLE = "TEAMSCALE_JAVA_PROFILER_WINDOWS_REGISTRY_TEST"
	}
}