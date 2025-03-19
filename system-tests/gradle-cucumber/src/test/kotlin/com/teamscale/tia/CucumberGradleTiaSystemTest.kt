package com.teamscale.tia

import com.teamscale.test.commons.SystemTestUtils
import com.teamscale.test.commons.SystemTestUtils.runGradle
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Runs cucumber tests in the nested `gradle-project` folder
 */
class CucumberGradleTiaSystemTest {
	private var teamscaleMockServer: TeamscaleMockServer? = null

	@BeforeEach
	@Throws(Exception::class)
	fun startFakeTeamscaleServer() {
		teamscaleMockServer = TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
			.withImpactedTests("hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers -1 & -10 #1")
			.acceptingReportUploads()
	}

	@AfterEach
	fun stopFakeTeamscaleServer() {
		teamscaleMockServer?.shutdown()
	}

	@Test
	@Throws(Exception::class)
	fun testImpactedCucumberTestsAreRun() {
		runGradle("gradle-project", "clean", "tiaTests", "--impacted", "teamscaleReportUpload", "--no-daemon")

		assertThat(teamscaleMockServer?.uploadedReports).hasSize(1)
		assertThat(teamscaleMockServer?.uploadedReports)
			.allMatch { it.partition == "Cucumber Tests" }

		val testReport = teamscaleMockServer?.parseUploadedTestwiseCoverageReport(0)
		// We can't just assert for testReport.test == 1 here because the Gradle plugin uploads all test cases.
		// The ones that were not executed have a mostly empty body, though. E.g.
		// {
		//    "uniformPath" : "hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers 10 & 15 #1",
		//    "paths" : [ ]
		//  }
		assertThat(testReport?.tests?.count { it.result != null })
			.isEqualTo(1)
	}
}
