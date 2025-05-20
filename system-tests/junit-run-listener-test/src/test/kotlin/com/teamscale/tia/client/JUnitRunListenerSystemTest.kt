package com.teamscale.tia.client

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import com.teamscale.test.commons.SystemTestUtils.runMavenTests
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.iterable.ThrowingExtractor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.function.Executable

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
		teamscaleMockServer?.reset()
	}

	/** Tests the JUnit 5 TestExecutionListener.  */
	@Test
	@Throws(Exception::class)
	fun testJUnit5TestExecutionListener() {
		runMavenTests("junit5-maven-project")

		val report: TestwiseCoverageReport = teamscaleMockServer!!.getOnlyTestwiseCoverageReport("part")
		assertThat(report.tests).hasSize(2)
		assertAll({
			assertThat(report.tests)
				.extracting<String> { it.uniformPath }
				.containsExactlyInAnyOrder("JUnit4ExecutedWithJUnit5Test/testAdd()", "JUnit5Test/testAdd()")
			assertThat(report.tests)
				.extracting<ETestExecutionResult> { it.result }
				.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED)
			assertThat(report.tests)
				.extracting<String> { it.coverage }
				.containsExactly("SystemUnderTest.java:3,6", "SystemUnderTest.java:3,6")
		})
	}

	/** Tests the JUnit 4 RunListener.  */
	@Test
	@Throws(Exception::class)
	fun testJUnit4RunListener() {
		runMavenTests("junit4-maven-project")

		val report = teamscaleMockServer!!.getOnlyTestwiseCoverageReport("part")
		assertThat(report.tests).hasSize(1)
		assertAll({
			Assertions.assertThat(report.tests)
				.extracting<String> { it.uniformPath }
				.containsExactlyInAnyOrder("JUnit4Test/testAdd")
			Assertions.assertThat(report.tests)
				.extracting<ETestExecutionResult> { it.result }
				.containsExactlyInAnyOrder(ETestExecutionResult.PASSED)
			Assertions.assertThat(report.tests)
				.extracting<String> { it.coverage }
				.containsExactly("SystemUnderTest.java:3,6")
		})
	}

	companion object {
		private var teamscaleMockServer: TeamscaleMockServer? = null
	}
}
