package com.teamscale.tia;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.Session;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
public class TiaMavenCucumberSystemTest {

	private static TeamscaleMockServer teamscaleMockServer = null;

	private static final String[] IMPACTED_TEST_PATHS = { // sorted alphabetically
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Actually we just want to test a http:\\/\\/link #1", // also tests addition, escaped /
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers #1",
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers #2",
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers #3",
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Add two numbers #4",
			"hellocucumber/RunCucumberTest/hellocucumber/calculator.feature/Subtract two numbers 99 & 99 #1"
	};

	private static final String COVERAGE_ADD = "Calculator.java:3,5;StepDefinitions.java:12,24-25,29-30,39-40";
	private static final String COVERAGE_SUBTRACT = "Calculator.java:3,9;StepDefinitions.java:12,24-25,34-35,39-40";

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
					.acceptingReportUploads()
					.withImpactedTests(IMPACTED_TEST_PATHS);
		}
		teamscaleMockServer.reset();
	}

	@AfterEach
	public void stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown();
	}

	@Test
	public void testMavenTia() throws Exception {
		SystemTestUtils.runMavenTests("maven-project");

		assertThat(teamscaleMockServer.allAvailableTests).extracting("partition").contains("MyPartition");

		Session session = teamscaleMockServer.getOnlySession();
		assertThat(session.getReports()).hasSize(1);
		assertThat(session.getPartition()).isEqualTo("MyPartition");

		TestwiseCoverageReport unitTestReport = session.getOnlyTestwiseCoverageReport();
		assertThat(unitTestReport.partial).isTrue();
		assertThat(unitTestReport.tests)
				.extracting(test -> test.uniformPath)
				.containsExactlyInAnyOrder(IMPACTED_TEST_PATHS);
		assertThat(unitTestReport.tests)
				.extracting(test -> test.result)
				.allMatch(result -> result == ETestExecutionResult.PASSED);
		assertThat(unitTestReport.tests)
				.extracting(SystemTestUtils::getCoverage)
				.containsExactlyInAnyOrder(COVERAGE_ADD, // must match TEST_PATHS
						COVERAGE_ADD,
						COVERAGE_ADD,
						COVERAGE_ADD,
						COVERAGE_ADD,
						COVERAGE_SUBTRACT);
	}

}
