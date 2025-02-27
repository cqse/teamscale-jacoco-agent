package com.teamscale.tia;

import com.teamscale.test.commons.SystemTestUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Checks that the plugin is compatible with the <a
 * href="https://docs.gradle.org/current/userguide/configuration_cache.html">Gradle configuration cache</a>.
 */
public class ConfigurationCacheTest {

	@Test
	public void testConfigurationCache() throws Exception {
		var result = SystemTestUtils.runGradle("gradle-project", "clean", "tiaTests", "--configuration-cache");
		Assertions.assertThat(result.getStdout()).contains("BUILD SUCCESSFUL");
		result = SystemTestUtils.runGradle("gradle-project", "clean", "tiaTests", "--configuration-cache");
		Assertions.assertThat(result.getStdout()).contains("Reusing configuration cache");
	}
}
