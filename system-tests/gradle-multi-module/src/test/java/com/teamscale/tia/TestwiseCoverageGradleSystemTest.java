package com.teamscale.tia;

import com.teamscale.client.EReportFormat;
import com.teamscale.report.compact.TeamscaleCompactCoverageReport;
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
	public void testGradleAggregatedTestwiseCoverageUploadWithoutJVMTestSuite() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "systemTest", "teamscaleSystemTestReportUpload");

		assertThat(teamscaleMockServer.uploadedReports).hasSize(2);
		assertThat(teamscaleMockServer.uploadedReports).allMatch(
				report -> report.getPartition().equals("System Tests"));

		TestwiseCoverageReport testwiseReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0);
		assertThat(testwiseReport.tests.getFirst().uniformPath).isEqualTo("com/example/app/MainTest/testMain()");
		assertThat(testwiseReport.tests.getFirst().paths).isNotEmpty();
		testwiseReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(1);
		assertThat(testwiseReport.tests.getFirst().uniformPath).isEqualTo("com/example/lib/CalculatorTest/testAdd()");
	}

	@Test
	public void testGradleAggregatedCompactCoverageUploadWithoutJVMTestSuite() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "unitTest", "teamscaleUnitTestReportUpload");

		assertThat(teamscaleMockServer.uploadedReports).hasSize(9);
		assertThat(teamscaleMockServer.uploadedReports).allMatch(report -> report.getPartition().equals("Unit Tests"));
		assertThat(teamscaleMockServer.uploadedReports.getFirst().getFormat()).isEqualTo(EReportFormat.TEAMSCALE_COMPACT_COVERAGE.name());
		assertThat(teamscaleMockServer.uploadedReports.stream()
				.filter(report -> EReportFormat.JUNIT.name().equals(report.getFormat())).count()).isEqualTo(8);

		TeamscaleCompactCoverageReport compactReport = teamscaleMockServer.parseUploadedCompactCoverageReport(0);
		assertThat(compactReport.getCoverage().getFirst().getFilePath()).isEqualTo("com/example/app/Main.java");
		assertThat(compactReport.getCoverage().getLast().getFilePath()).isEqualTo("com/example/lib/Calculator.java");
		assertThat(compactReport.getCoverage().getFirst().getFullyCoveredLines()).containsExactly(7, 8, 9);
		assertThat(compactReport.getCoverage().getLast().getFullyCoveredLines()).containsExactly(3, 6, 16);
	}

	@Test
	public void testGradleAggregatedCompactCoverageUploadWithJVMTestSuite() throws Exception {
		SystemTestUtils.runGradle("gradle-project", "clean", "test", "teamscaleTestReportUpload");

		assertThat(teamscaleMockServer.uploadedReports).hasSize(9);
		assertThat(teamscaleMockServer.uploadedReports.stream()
				.filter(report -> EReportFormat.JUNIT.name().equals(report.getFormat())).count()).isEqualTo(8);
		assertThat(teamscaleMockServer.uploadedReports).allMatch(
				report -> report.getPartition().equals("Default Tests"));

		TeamscaleCompactCoverageReport compactReport = teamscaleMockServer.parseUploadedCompactCoverageReport(0);
		assertThat(compactReport.getCoverage().getFirst().getFilePath()).isEqualTo("com/example/app/Main.java");
		assertThat(compactReport.getCoverage().getLast().getFilePath()).isEqualTo("com/example/lib/Calculator.java");
		assertThat(compactReport.getCoverage().getFirst().getFullyCoveredLines()).containsExactly(7, 8, 9);
		assertThat(compactReport.getCoverage().getLast().getFullyCoveredLines()).containsExactly(3, 6, 16);
	}

}
