package com.teamscale.tia;

import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Runs cucumber tests in the nested <code>gradle-project</code> folder
 */
public class CucumberGradleTiaSystemTest {

	private TeamscaleMockServer teamscaleMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
				.withImpactedTests("hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers -1 & -10 #1")
				.acceptingReportUploads();
	}

	@AfterEach
	public void stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown();
	}

	@Test
	public void testImpactedCucumberTestsAreRun() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "tiaTests", "teamscaleReportUpload", "--no-daemon");

		TestwiseCoverageReport testReport = teamscaleMockServer.getOnlyTestwiseCoverageReport("Cucumber Tests");
		// We can't just assert for testReport.test == 1 here because the Gradle plugin uploads all test cases.
		// The ones that were not executed have a mostly empty body, though. E.g.
		// {
		//    "uniformPath" : "hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers 10 & 15 #1",
		//    "paths" : [ ]
		//  }
		assertThat(testReport.tests.stream().filter(test -> test.result != null).count()).isEqualTo(1);
	}

}
