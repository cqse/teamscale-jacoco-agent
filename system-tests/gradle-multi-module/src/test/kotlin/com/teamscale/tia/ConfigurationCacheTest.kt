package com.teamscale.tia

import com.teamscale.test.commons.SystemTestUtils.runGradle
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Checks that the plugin is compatible with the [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
 */
class ConfigurationCacheTest {
	/** Configuration cache is enabled via gradle.properties  */
	@Test
	@Throws(Exception::class)
	fun testConfigurationCache() {
		var result = runGradle("gradle-project", "clean", "systemTest")
		assertThat(result.stdout).contains("BUILD SUCCESSFUL")
		result = runGradle("gradle-project", "clean", "systemTest")
		assertThat(result.stdout).contains("Reusing configuration cache")
	}
}
