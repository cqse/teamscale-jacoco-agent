package com.teamscale.tia;

import com.teamscale.client.EReportFormat;
import com.teamscale.report.compact.TeamscaleCompactCoverageReport;
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
				.acceptingReportUploads().withImpactedTests("com/example/app/MainTest/testMain()");
	}

	@AfterEach
	public void stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown();
	}

	@Test
	public void testGradleAggregatedTestwiseCoverageUploadWithoutJVMTestSuite() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "teamscaleSystemTestReportUpload");

		TestwiseCoverageReport testwiseReport = teamscaleMockServer.getOnlyTestwiseCoverageReport("System Tests");
		assertThat(testwiseReport.partial).isEqualTo(false);
		assertThat(testwiseReport.tests.getFirst().uniformPath).isEqualTo("com/example/app/MainTest/testMain()");
		assertThat(testwiseReport.tests.getLast().uniformPath).isEqualTo("com/example/lib/CalculatorTest/testAdd()");
		assertThat(testwiseReport.tests.getFirst().paths).isNotEmpty();
		assertThat(testwiseReport.tests.getLast().paths).isNotEmpty();
	}

	@Test
	public void testGradleAggregatedTestwiseCoverageUploadHasPartialFlagSet() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean",
				"systemTest", "-Dimpacted",
				"teamscaleSystemTestReportUpload");

		TestwiseCoverageReport testwiseReport = teamscaleMockServer.getOnlyTestwiseCoverageReport("System Tests");
		assertThat(testwiseReport.partial).isEqualTo(true);
		assertThat(testwiseReport.tests.getFirst().uniformPath).isEqualTo("com/example/app/MainTest/testMain()");
		assertThat(testwiseReport.tests.getLast().uniformPath).isEqualTo("com/example/lib/CalculatorTest/testAdd()");
		assertThat(testwiseReport.tests.getFirst().paths).isNotEmpty();
		assertThat(testwiseReport.tests.getLast().paths).isEmpty();
	}

	@Test
	public void testGradleAggregatedCompactCoverageUploadWithoutJVMTestSuite() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "unitTest", "teamscaleUnitTestReportUpload");

		Session session = teamscaleMockServer.getOnlySession("Unit Tests");
		assertThat(session.getReports()).hasSize(3);
		assertThat(session.getReports(EReportFormat.JUNIT)).hasSize(2);

		TeamscaleCompactCoverageReport compactReport = session.getCompactCoverageReport(0);
		assertThat(compactReport.getCoverage().getFirst().getFilePath()).isEqualTo("com/example/app/Main.java");
		assertThat(compactReport.getCoverage().getLast().getFilePath()).isEqualTo("com/example/lib/Calculator.java");
		assertThat(compactReport.getCoverage().getFirst().getFullyCoveredLines()).containsExactly(7, 8, 9);
		assertThat(compactReport.getCoverage().getLast().getFullyCoveredLines()).containsExactly(3, 6, 16);
	}

	@Test
	public void testGradleAggregatedCompactCoverageUploadWithJVMTestSuite() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "teamscaleTestReportUpload");

		Session session = teamscaleMockServer.getOnlySession("Default Tests");
		assertThat(session.getReports()).hasSize(3);
		assertThat(session.getReports(EReportFormat.JUNIT)).hasSize(2);

		TeamscaleCompactCoverageReport compactReport = session.getCompactCoverageReport(0);
		assertThat(compactReport.getCoverage().getFirst().getFilePath()).isEqualTo("com/example/app/Main.java");
		assertThat(compactReport.getCoverage().getLast().getFilePath()).isEqualTo("com/example/lib/Calculator.java");
		assertThat(compactReport.getCoverage().getFirst().getFullyCoveredLines()).containsExactly(7, 8, 9);
		assertThat(compactReport.getCoverage().getLast().getFullyCoveredLines()).containsExactly(3, 6, 16);
	}

}
