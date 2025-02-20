package com.teamscale

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Checks that the plugin is compatible with the <a href="https://docs.gradle.org/current/userguide/configuration_cache.html">Gradle configuration cache</a>. */
class ConfigurationCacheFuncTest : TeamscalePluginTestBase() {

	@BeforeEach
	fun init() {
		rootProject.withSampleCode()
		rootProject.defaultProjectSetup()
	}

	@Test
	fun testConfigurationCache() {
		rootProject.defineLegacyUnitTestTask()

		run("unitTest", "--configuration-cache")
		val build = run("unitTest", "--configuration-cache")
		assertThat(build.output).contains("Reusing configuration cache.")
	}
}
