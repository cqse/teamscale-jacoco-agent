package com.teamscale.tia

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.coverage
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

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
class TiaMavenSystemTest {
	private lateinit var teamscaleMockServer: TeamscaleMockServer

	@BeforeEach
	@Throws(Exception::class)
	fun startFakeTeamscaleServer() {
		teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
			.withAuthentication("build", "6lJKEvNHeTxGPhMAi4D84DWqzoSFL1p4")
			.acceptingReportUploads()
			.withImpactedTests(
				"bar/UnitTest/utBla()", "bar/UnitTest/utFoo()",
				"bar/IntegIT/itBla()", "bar/IntegIT/itFoo()"
			)
	}

	@AfterEach
	fun stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown()
	}

	@Test
	@Throws(Exception::class)
	fun testMavenTia() {
		runMavenTests("maven-project")

		assertThat(teamscaleMockServer.availableTests).extracting("partition").contains("MyPartition")

		assertThat(teamscaleMockServer.uploadedReports).hasSize(2)

		assertThat(teamscaleMockServer.impactedTestRepositories).containsExactly("myRepoId", "myRepoId")
		assertThat(teamscaleMockServer.uploadRepositories).containsExactly("myRepoId", "myRepoId")

		assertThat(teamscaleMockServer.impactedTestCommits.firstOrNull()).matches("abcd1337, .*")
		assertThat(teamscaleMockServer.impactedTestCommits.getOrNull(1)).matches("abcd1337, .*")
		assertThat(teamscaleMockServer.uploadCommits.firstOrNull()).matches("abcd1337, .*")
		assertThat(teamscaleMockServer.uploadCommits.getOrNull(1)).matches("abcd1337, .*")

		val unitTestReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0)
		assertThat(unitTestReport.tests).hasSize(2)
		assertThat(unitTestReport.partial).isTrue()
		assertAll({
			assertThat(unitTestReport.tests)
				.extracting<String, RuntimeException> { it.uniformPath }
				.containsExactlyInAnyOrder("bar/UnitTest/utBla()", "bar/UnitTest/utFoo()")
			assertThat(unitTestReport.tests)
				.extracting<ETestExecutionResult, RuntimeException> { it.result }
				.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED)
			assertThat(unitTestReport.tests.map { it.coverage })
				.containsExactly("SUT.java:3,6-7", "SUT.java:3,10-11")
		})

		val integrationTestReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(1)
		assertThat(integrationTestReport.tests).hasSize(2)
		assertAll({
			assertThat(integrationTestReport.tests)
				.extracting<String, RuntimeException> { it.uniformPath }
				.containsExactlyInAnyOrder("bar/IntegIT/itBla()", "bar/IntegIT/itFoo()")
			assertThat(integrationTestReport.tests)
				.extracting<ETestExecutionResult, RuntimeException> { it.result }
				.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED)
			assertThat(integrationTestReport.tests.map { it.coverage })
				.containsExactly("SUT.java:3,6-7", "SUT.java:3,10-11")
		})
	}

	@Test
	@Throws(IOException::class)
	fun testPreferBranchAndTimestampOverRevisionWhenProvidedManually() {
		runMavenTests("maven-project", "-DteamscaleRevision=abcd1337", "-DteamscaleTimestamp=master:HEAD")

		assertThat(teamscaleMockServer.impactedTestCommits[0]).matches("abcd1337, null")
		assertThat(teamscaleMockServer.impactedTestCommits[1]).matches("abcd1337, null")
		assertThat(teamscaleMockServer.uploadCommits[0]).matches("abcd1337, null")
		assertThat(teamscaleMockServer.uploadCommits[1]).matches("abcd1337, null")
	}

	@Test
	@Throws(IOException::class)
	fun testBaselineRevisionIsPreferred() {
		runMavenTests("maven-project", "-DbaselineRevision=rev1", "-DbaselineCommit=master:1234")

		assertThat(teamscaleMockServer.baselines).containsExactly("rev1, null", "rev1, null")
	}

	@Test
	@Throws(IOException::class)
	fun testBaselineCommitIsUsed() {
		runMavenTests("maven-project", "-DbaselineCommit=master:1234")

		assertThat(teamscaleMockServer.baselines).containsExactly("null, master:1234", "null, master:1234")
	}
}
