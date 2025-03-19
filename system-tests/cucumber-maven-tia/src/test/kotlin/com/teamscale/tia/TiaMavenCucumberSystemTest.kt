package com.teamscale.tia

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import com.teamscale.test.commons.SystemTestUtils.runMavenTests
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.util.*

/**
 * Runs several Maven projects' Surefire tests that have the agent attached, and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
class TiaMavenCucumberSystemTest {
	private var teamscaleMockServer: TeamscaleMockServer? = null

	@BeforeEach
	@Throws(Exception::class)
	fun startFakeTeamscaleServer() {
		teamscaleMockServer?.uploadedReports?.clear() ?: run {
			teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
				.acceptingReportUploads()
				.withImpactedTests(*IMPACTED_TEST_PATHS)
		}
	}

	@AfterEach
	fun stopFakeTeamscaleServer() {
		teamscaleMockServer?.shutdown()
	}

	@Test
	@Throws(Exception::class)
	fun testMavenTia() {
		runMavenTests("maven-project")

		// Check that the partition is correctly recognized
		assertThat(teamscaleMockServer?.availableTests)
			.extracting("partition")
			.contains("MyPartition")

		// Expect a single report upload
		assertThat(teamscaleMockServer?.uploadedReports).hasSize(1)

		val unitTestReport = teamscaleMockServer?.parseUploadedTestwiseCoverageReport(0)
		assertThat(unitTestReport?.tests).hasSize(IMPACTED_TEST_PATHS.size)
		assertThat(unitTestReport?.partial).isTrue()

		// Bundle checks in assertAll to gather all assertions
		assertAll(
			Executable {
				// Check uniform paths in any order
				assertThat(unitTestReport?.tests)
					.extracting<String> { it.uniformPath }
					.containsExactlyInAnyOrder(*IMPACTED_TEST_PATHS)
			},
			Executable {
				// Verify that all test results are PASSED
				assertThat(unitTestReport?.tests)
					.extracting<ETestExecutionResult> { it.result }
					.containsExactlyInAnyOrder(*IMPACTED_TEST_PATHS.map { ETestExecutionResult.PASSED }.toTypedArray())
			},
			Executable {
				// Ensure coverage strings match the expected values
				assertThat(unitTestReport?.tests)
					.extracting<String> { it.coverage }
					.containsExactly(
						COVERAGE_ADD,
						COVERAGE_ADD,
						COVERAGE_ADD,
						COVERAGE_ADD,
						COVERAGE_ADD,
						COVERAGE_SUBTRACT
					)
			}
		)
	}

	companion object {
		private val IMPACTED_TEST_PATHS = arrayOf( // sorted alphabetically
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Actually we just want to test a http:\\/\\/link #1",  // also tests addition, escaped /
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers #1",
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers #2",
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers #3",
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers #4",
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Subtract two numbers 99 & 99 #1"
		)

		private const val COVERAGE_ADD = "Calculator.java:3,5;StepDefinitions.java:12,24-25,29-30,39-40"
		private const val COVERAGE_SUBTRACT = "Calculator.java:3,9;StepDefinitions.java:12,24-25,34-35,39-40"
	}
}
