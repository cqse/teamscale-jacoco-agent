package com.teamscale.profiler.installer.windows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests our registry access class.
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsRegistryTest {

	private static final String VARIABLE = "TEAMSCALE_JAVA_PROFILER_WINDOWS_REGISTRY_TEST";

	@BeforeEach
	void clearRegistry() throws Exception {
		Runtime.getRuntime()
				.exec(new String[]{"reg", "delete", "HKLM\\" + WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, "/v", VARIABLE});
	}

	@Test
	void testAllFunctions() throws Exception {
		assertThat(WindowsRegistry.INSTANCE.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE)).isNull();

		WindowsRegistry.INSTANCE.setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE, "foobar");
		assertThat(WindowsRegistry.INSTANCE.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE)).isEqualTo(
				"foobar");

		WindowsRegistry.INSTANCE.setHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE, "goo");
		assertThat(WindowsRegistry.INSTANCE.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE)).isEqualTo(
				"goo");

		WindowsRegistry.INSTANCE.deleteHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE);
		assertThat(WindowsRegistry.INSTANCE.getHklmValue(WindowsRegistry.ENVIRONMENT_REGISTRY_KEY, VARIABLE)).isNull();
	}

}