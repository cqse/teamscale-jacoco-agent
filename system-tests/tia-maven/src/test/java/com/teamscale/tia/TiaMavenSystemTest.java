package com.teamscale.tia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
public class TiaMavenSystemTest {

	private static TeamscaleMockServer teamscaleMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT).acceptingReportUploads()
					.withImpactedTests("bar/UnitTest/utBla()", "bar/UnitTest/utFoo()",
							"bar/IntegIT/itBla()", "bar/IntegIT/itFoo()");
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

		assertThat(teamscaleMockServer.uploadedReports).hasSize(2);

		assertThat(teamscaleMockServer.impactedTestRepositories).containsExactly("myRepoId", "myRepoId");
		assertThat(teamscaleMockServer.uploadRepositories).containsExactly("myRepoId", "myRepoId");

		assertThat(teamscaleMockServer.impactedTestCommits.get(0)).matches("abcd1337, .*");
		assertThat(teamscaleMockServer.impactedTestCommits.get(1)).matches("abcd1337, .*");
		assertThat(teamscaleMockServer.uploadCommits.get(0)).matches("abcd1337, .*");
		assertThat(teamscaleMockServer.uploadCommits.get(1)).matches("abcd1337, .*");

		TestwiseCoverageReport unitTestReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0);
		assertThat(unitTestReport.tests).hasSize(2);
		assertThat(unitTestReport.partial).isTrue();
		assertAll(() -> {
			assertThat(unitTestReport.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder("bar/UnitTest/utBla()", "bar/UnitTest/utFoo()");
			assertThat(unitTestReport.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED);
			assertThat(unitTestReport.tests).extracting(SystemTestUtils::getCoverageString)
					.containsExactly("SUT.java:3,6-7", "SUT.java:3,10-11");
		});

		TestwiseCoverageReport integrationTestReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(1);
		assertThat(integrationTestReport.tests).hasSize(2);
		assertAll(() -> {
			assertThat(integrationTestReport.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder("bar/IntegIT/itBla()", "bar/IntegIT/itFoo()");
			assertThat(integrationTestReport.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED);
			assertThat(integrationTestReport.tests).extracting(SystemTestUtils::getCoverageString)
					.containsExactly("SUT.java:3,6-7", "SUT.java:3,10-11");
		});
	}

}
