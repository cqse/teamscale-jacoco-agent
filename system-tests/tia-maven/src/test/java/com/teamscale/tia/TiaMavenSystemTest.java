package com.teamscale.tia;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Runs several Maven projects' Surefire tests that have the agent attached and one of our JUnit run listeners enabled.
 * Checks that this produces a correct coverage report.
 */
public class TiaMavenSystemTest {

	private static TeamscaleMockServer teamscaleMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		teamscaleMockServer = new TeamscaleMockServer(SystemTestUtils.TEAMSCALE_PORT)
				.withAuthentication("build", "6lJKEvNHeTxGPhMAi4D84DWqzoSFL1p4")
				.acceptingReportUploads()
				.withImpactedTests("bar/UnitTest/utBla()", "bar/UnitTest/utFoo()",
						"bar/IntegIT/itBla()", "bar/IntegIT/itFoo()");
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
			assertThat(unitTestReport.tests).extracting(SystemTestUtils::getCoverage)
					.containsExactly("SUT.java:3,6-7", "SUT.java:3,10-11");
		});

		TestwiseCoverageReport integrationTestReport = teamscaleMockServer.parseUploadedTestwiseCoverageReport(1);
		assertThat(integrationTestReport.tests).hasSize(2);
		assertAll(() -> {
			assertThat(integrationTestReport.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder("bar/IntegIT/itBla()", "bar/IntegIT/itFoo()");
			assertThat(integrationTestReport.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED);
			assertThat(integrationTestReport.tests).extracting(SystemTestUtils::getCoverage)
					.containsExactly("SUT.java:3,6-7", "SUT.java:3,10-11");
		});
	}

	@Test
	public void testPreferBranchAndTimestampOverRevisionWhenProvidedManually() throws IOException {
		SystemTestUtils.runMavenTests("maven-project", "-DteamscaleRevision=abcd1337", "-DteamscaleTimestamp=master:HEAD");

		assertThat(teamscaleMockServer.impactedTestCommits.get(0)).matches("abcd1337, null");
		assertThat(teamscaleMockServer.impactedTestCommits.get(1)).matches("abcd1337, null");
		assertThat(teamscaleMockServer.uploadCommits.get(0)).matches("abcd1337, null");
		assertThat(teamscaleMockServer.uploadCommits.get(1)).matches("abcd1337, null");
	}

	@Test
	public void testBaselineRevisionIsPreferred() throws IOException {
		SystemTestUtils.runMavenTests("maven-project", "-DbaselineRevision=rev1", "-DbaselineCommit=master:1234");

		assertThat(teamscaleMockServer.baselines).containsExactly("rev1, null", "rev1, null");
	}

	@Test
	public void testBaselineCommitIsUsed() throws IOException {
		SystemTestUtils.runMavenTests("maven-project", "-DbaselineCommit=master:1234");

		assertThat(teamscaleMockServer.baselines).containsExactly("null, master:1234", "null, master:1234");
	}

}
