package com.teamscale.tia.client

import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import testframework.CustomTestFramework

/**
 * Runs the [CustomTestFramework] from src/main with our agent attached in tia-mode=http (the agent is configured
 * in this project's build.gradle). The custom test framework contains an integration with the tia-client. Asserts that
 * the resulting [TestInfo]s look as expected.
 *
 *
 * This ensures that our tia-client library doesn't break unexpectedly.
 */
class TiaClientTiaModeHttpSystemTest {
	@Test
	@Throws(Exception::class)
	fun systemTest() {
		val customTestFramework = CustomTestFramework(SystemTestUtils.AGENT_PORT)
		customTestFramework.runTestsWithTia()

		assertThat(customTestFramework.testInfos.map { it.coverage })
			.containsExactlyInAnyOrder("SystemUnderTest.kt:4,6", "SystemUnderTest.kt:4,9")
	}
}
