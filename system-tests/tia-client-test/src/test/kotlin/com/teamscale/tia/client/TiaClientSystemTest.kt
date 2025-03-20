package com.teamscale.tia.client

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import com.teamscale.test.commons.TeamscaleMockServer
import com.teamscale.tia.client.TestRun.TestResultWithMessage
import okhttp3.HttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import systemundertest.SystemUnderTest
import testframework.CustomTestFramework

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
	@Throws(Exception::class)
	fun startFakeTeamscaleServer() {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads()
				.withImpactedTests("testFoo", "testBar")
		}
		teamscaleMockServer?.uploadedReports?.clear()
	}

	@Test
	@Throws(Exception::class)
	fun systemTest() {
		val customTestFramework = CustomTestFramework(SystemTestUtils.AGENT_PORT)
		customTestFramework.runTestsWithTia()

		assertThat(teamscaleMockServer?.uploadedReports).hasSize(1)

		val report = teamscaleMockServer?.parseUploadedTestwiseCoverageReport(0)
		assertThat(report?.tests)
			.hasSize(2)
			.satisfiesExactlyInAnyOrder(
				{ test ->
					assertThat(test.uniformPath).isEqualTo("testFoo")
					assertThat(test.result).isEqualTo(ETestExecutionResult.PASSED)
					assertThat(test.coverage).isEqualTo("SystemUnderTest.kt:4,6")
				},
				{ test ->
					assertThat(test.uniformPath).isEqualTo("testBar")
					assertThat(test.result).isEqualTo(ETestExecutionResult.FAILURE)
					assertThat(test.coverage).isEqualTo("SystemUnderTest.kt:4,9")
				}
			)
	}

	@Test
	@Throws(Exception::class)
	fun systemTestWithSpecialCharacter() {
		val agent = TiaAgent(false, HttpUrl.get("http://localhost:${SystemTestUtils.AGENT_PORT}"))
		val testRun = agent.startTestRunWithoutTestSelection()

		val uniformPath = "my/strange;te st[(-+path!@#$%^&*(name"
		val runningTest = testRun.startTest(uniformPath)
		SystemUnderTest().foo()
		runningTest.endTest(TestResultWithMessage(ETestExecutionResult.PASSED, ""))

		testRun.endTestRun(true)

		assertThat(teamscaleMockServer?.uploadedReports).hasSize(1)

		val report = teamscaleMockServer?.parseUploadedTestwiseCoverageReport(0)
		assertThat(report?.tests).hasSize(1)
		assertThat(report?.tests).element(0).extracting { it.uniformPath }
			.isEqualTo(uniformPath)
	}

	companion object {
		private var teamscaleMockServer: TeamscaleMockServer? = null

		@JvmStatic
		@AfterAll
		fun stopFakeTeamscaleServer() {
			teamscaleMockServer?.shutdown()
		}
	}
}
