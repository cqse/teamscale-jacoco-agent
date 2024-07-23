package com.teamscale.tia;

import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Runs tests in all submodules and expects the results of both in the upload.
 */
public class CucumberGradleTiaSystemTest {

	private TeamscaleMockServer teamscaleMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
				.acceptingReportUploads();
	}

	@AfterEach
	public void stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown();
	}

	@Test
	public void testCucumberTestsAreExecutedAndUploaded() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "tiaTests", "teamscaleReportUpload", "--no-daemon");

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);
		assertThat(teamscaleMockServer.uploadedReports).allMatch(report -> report.getPartition().equals("Cucumber Tests"));

		TestwiseCoverageReport testReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0);
		assertThat(testReport.tests).hasSize(4);
		assertThat(testReport.tests).allMatch(test -> test.uniformPath.startsWith("hellocucumber/RunCucumberTest/hellocucumber/calculator.feature"));
	}

}
