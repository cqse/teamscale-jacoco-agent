package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
public class JUnitRunListenerSystemTest {

	private static TeamscaleMockServer teamscaleMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads();
		}
		teamscaleMockServer.reset();
	}

	/** Tests the JUnit 5 TestExecutionListener. */
	@Test
	public void testJUnit5TestExecutionListener() throws Exception {
		SystemTestUtils.runMavenTests("junit5-maven-project");

		TestwiseCoverageReport report = teamscaleMockServer.getOnlyTestwiseCoverageReport("part");
		assertThat(report.tests).hasSize(2);
		assertAll(() -> {
			assertThat(report.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder("JUnit4ExecutedWithJUnit5Test/testAdd()", "JUnit5Test/testAdd()");
			assertThat(report.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED);
			assertThat(report.tests).extracting(SystemTestUtils::getCoverageString)
					.containsExactly("SystemUnderTest.java:3,6", "SystemUnderTest.java:3,6");
		});
	}

	/** Tests the JUnit 4 RunListener. */
	@Test
	public void testJUnit4RunListener() throws Exception {
		SystemTestUtils.runMavenTests("junit4-maven-project");

		TestwiseCoverageReport report = teamscaleMockServer.getOnlyTestwiseCoverageReport("part");
		assertThat(report.tests).hasSize(1);
		assertAll(() -> {
			assertThat(report.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder("JUnit4Test/testAdd");
			assertThat(report.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.PASSED);
			assertThat(report.tests).extracting(SystemTestUtils::getCoverageString)
					.containsExactly("SystemUnderTest.java:3,6");
		});
	}

}
