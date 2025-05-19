package com.teamscale.tia

import com.teamscale.client.TestWithClusterId
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.test.commons.ExternalReport
import com.teamscale.test.commons.Session
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import com.teamscale.test.commons.SystemTestUtils.runMavenTests
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.iterable.ThrowingExtractor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.function.Predicate

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
class TiaMavenCucumberSystemTest {
	private lateinit var teamscaleMockServer: TeamscaleMockServer

	@BeforeEach
	@Throws(Exception::class)
	fun startFakeTeamscaleServer() {
		teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
			.acceptingReportUploads()
			.withImpactedTests(*IMPACTED_TEST_PATHS)
	}

	@AfterEach
	fun stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown()
	}

	@Test
	@Throws(Exception::class)
	fun testMavenTia() {
		runMavenTests("maven-project")

		assertThat(teamscaleMockServer.allAvailableTests)
			.extracting("partition")
			.contains("MyPartition")

		val session = teamscaleMockServer.onlySession
		assertThat(session.getReports()).hasSize(1)
		assertThat(session.partition).isEqualTo("MyPartition")

		val unitTestReport = session.onlyTestwiseCoverageReport
		with(unitTestReport) {
			assertThat(partial).isTrue()
			assertThat(tests)
				.extracting<String> { it.uniformPath }
				.containsExactlyInAnyOrder(*IMPACTED_TEST_PATHS)
			assertThat(tests)
				.extracting<ETestExecutionResult> { it.result }
				.allMatch { it == ETestExecutionResult.PASSED }
			assertThat(tests)
				.extracting<String> { it.coverage }
				.containsExactlyInAnyOrder(
					COVERAGE_ADD,  // must match TEST_PATHS
					COVERAGE_ADD,
					COVERAGE_ADD,
					COVERAGE_ADD,
					COVERAGE_ADD,
					COVERAGE_SUBTRACT
				)
		}
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
