package com.teamscale.tia.client

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import com.teamscale.test.commons.SystemTestUtils.runMavenTests
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
class JUnitRunListenerSystemTest {
	@BeforeEach
	@Throws(Exception::class)
	fun startFakeTeamscaleServer() {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads()
		}
		teamscaleMockServer?.uploadedReports?.clear()
	}

	/** Tests the JUnit 5 TestExecutionListener.  */
	@Test
	@Throws(Exception::class)
	fun testJUnit5TestExecutionListener() {
		runMavenTests("junit5-maven-project")

		assertThat(teamscaleMockServer?.uploadedReports).hasSize(1)

		val report = teamscaleMockServer?.parseUploadedTestwiseCoverageReport(0)
		assertThat(report?.tests)
			.hasSize(2)
			.satisfiesExactlyInAnyOrder(
				{ test ->
					assertThat(test.uniformPath).isEqualTo("JUnit4ExecutedWithJUnit5Test/testAdd()")
					assertThat(test.result).isEqualTo(ETestExecutionResult.PASSED)
					assertThat(test.coverage).isEqualTo("SystemUnderTest.java:3,6")
				},
				{ test ->
					assertThat(test.uniformPath).isEqualTo("JUnit5Test/testAdd()")
					assertThat(test.result).isEqualTo(ETestExecutionResult.PASSED)
					assertThat(test.coverage).isEqualTo("SystemUnderTest.java:3,6")
				}
			)
	}

	/** Tests the JUnit 4 RunListener.  */
	@Test
	@Throws(Exception::class)
	fun testJUnit4RunListener() {
		runMavenTests("junit4-maven-project")

		assertThat(teamscaleMockServer?.uploadedReports).hasSize(1)

		val report = teamscaleMockServer?.parseUploadedTestwiseCoverageReport(0)
		assertThat(report?.tests)
			.hasSize(1)
			.satisfiesExactly(
				{ test ->
					assertThat(test.uniformPath).isEqualTo("JUnit4Test/testAdd")
					assertThat(test.result).isEqualTo(ETestExecutionResult.PASSED)
					assertThat(test.coverage).isEqualTo("SystemUnderTest.java:3,6")
				}
			)
	}

	companion object {
		private var teamscaleMockServer: TeamscaleMockServer? = null
	}
}
