package com.teamscale.tia

import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.runGradle
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Runs tests in all submodules and expects the results of both in the upload.
 */
class TestwiseCoverageGradleSystemTest {
	private var teamscaleMockServer: TeamscaleMockServer? = null

	@BeforeEach
	@Throws(Exception::class)
	fun startFakeTeamscaleServer() {
		teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
			.acceptingReportUploads()
	}

	@AfterEach
	fun stopFakeTeamscaleServer() {
		teamscaleMockServer?.shutdown()
	}

	@Test
	@Throws(Exception::class)
	fun testGradleTestwiseCoverageUpload() {
		runGradle("gradle-project", "clean", "tiaTests", "teamscaleReportUpload", "--no-daemon")

		assertThat(teamscaleMockServer?.uploadedReports).hasSize(2)
		assertThat(teamscaleMockServer?.uploadedReports)
			.allMatch { it.partition == "Unit Tests" }

		var unitTestReport = teamscaleMockServer?.parseUploadedTestwiseCoverageReport(0)
		assertThat(unitTestReport?.tests?.firstOrNull()?.uniformPath).isEqualTo("org/example/Test1/test1()")
		unitTestReport = teamscaleMockServer?.parseUploadedTestwiseCoverageReport(1)
		assertThat(unitTestReport?.tests?.firstOrNull()?.uniformPath).isEqualTo("org/example/Test2/test2()")
	}
}
