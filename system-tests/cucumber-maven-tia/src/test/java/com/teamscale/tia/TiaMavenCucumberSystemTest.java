package com.teamscale.tia;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
public class TiaMavenCucumberSystemTest {

	/**
	 * This port must match what is configured for the -javaagent line in the corresponding POM of the Maven test
	 * project.
	 */
	private static final int FAKE_TEAMSCALE_PORT = 63800;
	private static TeamscaleMockServer teamscaleMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = new TeamscaleMockServer(FAKE_TEAMSCALE_PORT).acceptingReportUploads()
					.withImpactedTests(
							"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers 0 & 0/Add two numbers 0 & 0",
							"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers/Example #1.1",
							"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers/Example #1.2");
		}
		teamscaleMockServer.uploadedReports.clear();
	}

	@AfterEach
	public void stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown();
	}

	@Test
	public void testMavenTia() throws Exception {
		SystemTestUtils.runMavenTests("maven-project");

		assertThat(teamscaleMockServer.availableTests).extracting("partition").contains("MyPartition");

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);

		TestwiseCoverageReport unitTestReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0);
		assertThat(unitTestReport.tests).hasSize(3);
		assertThat(unitTestReport.partial).isTrue();
		assertAll(() -> {
			assertThat(unitTestReport.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder(
							"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers 0 & 0/Add two numbers 0 & 0",
							"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers/Example #1.1",
							"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers/Example #1.2"
					);
			assertThat(unitTestReport.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED,
							ETestExecutionResult.PASSED);
			assertThat(unitTestReport.tests).extracting(SystemTestUtils::getCoverageString)
					.containsExactly(
							"Calculator.java:3,5;StepDefinitions.java:12,24-25,29-30,39-40",
							"Calculator.java:3,5;StepDefinitions.java:12,24-25,29-30,39-40",
							"Calculator.java:3,5;StepDefinitions.java:12,24-25,29-30,39-40"
					);
		});
	}

}
