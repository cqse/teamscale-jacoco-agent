package com.teamscale

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


/**
 * Integration tests for the Teamscale Gradle plugin.
 */
class TeamscalePluginTest : TeamscalePluginTestBase() {

	@BeforeEach
	fun init() {
		rootProject.withSampleCode()
		rootProject.defaultProjectSetup()
	}

	@Test
	fun `teamscale plugin can be configured`() {
		rootProject.defineLegacyUnitTestTask()

		assertThat(
			run(  "clean", "tasks").output
		).contains("SUCCESS")
	}

	@Test
	fun `unit tests can be executed normally`() {
		rootProject.excludeFailingTests()
		rootProject.defineLegacyUnitTestTask()

		assertThat(
			run(  "clean", "unitTest").output
		).contains("SUCCESS (18 tests, 12 successes, 0 failures, 6 skipped)")
	}
}
