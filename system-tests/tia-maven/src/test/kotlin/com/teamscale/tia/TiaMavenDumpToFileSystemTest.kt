package com.teamscale.tia

import com.teamscale.client.JsonUtils.deserialize
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
import com.teamscale.test.commons.SystemTestUtils.getReportFileNames
import com.teamscale.test.commons.SystemTestUtils.runMavenTests
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.function.Executable
import java.io.IOException
import java.nio.file.Files

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
class TiaMavenDumpToFileSystemTest {
	@BeforeEach
	@Throws(Exception::class)
	fun startFakeTeamscaleServer() {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).disallowingStateChanges()
		}
	}

	@AfterEach
	fun stopFakeTeamscaleServer() {
		teamscaleMockServer?.shutdown()
	}

	@Test
	@Throws(Exception::class)
	fun testMavenTia() {
		runMavenTests(MAVEN_PROJECT_NAME)

		val unitTestReport = parseDumpedCoverageReport(0)
		assertThat(unitTestReport.tests).hasSize(2)
		assertThat(unitTestReport.partial).isFalse()
		assertAll({
			assertThat(unitTestReport.tests)
				.extracting<String, RuntimeException> { test: TestInfo -> test.uniformPath }
				.containsExactlyInAnyOrder("bar/UnitTest/utBla()", "bar/UnitTest/utFoo()")
			assertThat(unitTestReport.tests)
				.extracting<ETestExecutionResult, RuntimeException> { it.result }
				.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED)
			assertThat(unitTestReport.tests.map { it.coverage })
				.containsExactly("SUT.java:3,6-7", "SUT.java:3,10-11")
		})

		val integrationTestReport = parseDumpedCoverageReport(1)
		assertThat(integrationTestReport.tests).hasSize(2)
		assertAll({
			assertThat(integrationTestReport.tests)
				.extracting<String, RuntimeException> { test: TestInfo -> test.uniformPath }
				.containsExactlyInAnyOrder("bar/IntegIT/itBla()", "bar/IntegIT/itFoo()")
			assertThat(integrationTestReport.tests)
				.extracting<ETestExecutionResult, RuntimeException> { test: TestInfo -> test.result }
				.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED)
			assertThat(integrationTestReport.tests.map { it.coverage })
				.containsExactly("SUT.java:3,6-7", "SUT.java:3,10-11")
		})
	}

	@Throws(IOException::class)
	private fun parseDumpedCoverageReport(index: Int): TestwiseCoverageReport {
		val files = getReportFileNames(MAVEN_PROJECT_NAME)
		return deserialize<TestwiseCoverageReport>(
			String(Files.readAllBytes(files[index]))
		)
	}

	companion object {
		private const val MAVEN_PROJECT_NAME = "maven-dump-local-project"

		private var teamscaleMockServer: TeamscaleMockServer? = null
	}
}
