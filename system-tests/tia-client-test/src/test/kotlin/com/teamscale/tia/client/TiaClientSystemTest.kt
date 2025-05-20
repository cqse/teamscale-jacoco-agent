package com.teamscale.tia.client

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import com.teamscale.test.commons.TeamscaleMockServer
import com.teamscale.tia.client.TestRun.TestResultWithMessage
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.iterable.ThrowingExtractor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.function.Executable
import systemundertest.SystemUnderTest
import testframework.CustomTestFramework
import java.util.function.Function

/**
 * Runs the custom test framework from src/main with our agent attached (the agent is configured in this project's
 * build.gradle). The custom test framework contains an integration via the tia-client against the
 * [TeamscaleMockServer]. Asserts that the resulting report looks as expected.
 *
 *
 * This ensures that our tia-client library doesn't break unexpectedly.
 */
class TiaClientSystemTest {
	@BeforeEach
	fun startFakeTeamscaleServer() {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads()
				.withImpactedTests("testFoo", "testBar")
		}
		teamscaleMockServer?.reset()
	}

	@Test
	@Throws(Exception::class)
	fun systemTest() {
		val customTestFramework = CustomTestFramework(SystemTestUtils.AGENT_PORT)
		customTestFramework.runTestsWithTia()

		val report = teamscaleMockServer!!.getOnlyTestwiseCoverageReport("part")
		assertThat(report.tests).hasSize(2)
		assertAll({
			assertThat(report.tests)
				.extracting<String> { it.uniformPath }
				.containsExactlyInAnyOrder("testBar", "testFoo")
			assertThat(report.tests)
				.extracting<ETestExecutionResult> { it.result }
				.containsExactlyInAnyOrder(ETestExecutionResult.FAILURE, ETestExecutionResult.PASSED)
			assertThat(report.tests)
				.extracting<String> { it.coverage }
				.containsExactlyInAnyOrder("SystemUnderTest.kt:4,9", "SystemUnderTest.kt:4,6")
		})
	}

	@Test
	@Throws(Exception::class)
	fun systemTestWithSpecialCharacter() {
		val agent = TiaAgent(false, "http://localhost:${SystemTestUtils.AGENT_PORT}".toHttpUrl())
		val testRun = agent.startTestRunWithoutTestSelection()

		val uniformPath = "my/strange;te st[(-+path!@#$%^&*(name"
		val runningTest = testRun.startTest(uniformPath)
		SystemUnderTest().foo()
		runningTest.endTest(TestResultWithMessage(ETestExecutionResult.PASSED, ""))

		testRun.endTestRun(true)

		val report = teamscaleMockServer!!.getOnlyTestwiseCoverageReport("part")
		assertThat(report.tests).hasSize(1)
		assertThat(report.tests).element(0)
			.extracting { it.uniformPath }.isEqualTo(uniformPath)
	}

	companion object {
		private var teamscaleMockServer: TeamscaleMockServer? = null

		@JvmStatic
		@AfterAll
		fun stopFakeTeamscaleServer() {
			teamscaleMockServer!!.shutdown()
		}
	}
}
