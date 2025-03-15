package com.teamscale.tia;

import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.Session;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs tests in all submodules and expects the results of both in the upload.
 */
public class TestwiseCoverageGradleSystemTest {

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
	public void testGradleTestwiseCoverageUpload() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "tiaTests", "teamscaleReportUpload", "--no-daemon");

		Session session = teamscaleMockServer.getOnlySession("Unit Tests");
		assertThat(session.getReports()).hasSize(2);

		TestwiseCoverageReport unitTestReport = session.getTestwiseCoverageReport(0);
		assertThat(unitTestReport.tests.getFirst().uniformPath).isEqualTo("org/example/Test1/test1()");
		unitTestReport = session.getTestwiseCoverageReport(1);
		assertThat(unitTestReport.tests.getFirst().uniformPath).isEqualTo("org/example/Test2/test2()");
	}

}
