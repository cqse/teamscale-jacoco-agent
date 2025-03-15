package com.teamscale.tia;

import com.teamscale.client.JsonUtils;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.Session;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
		SystemTestUtils.runMavenTests("maven-project", "-Dtia");

		assertThat(teamscaleMockServer.availableTests).extracting("partition").containsOnly("Unit Tests", "Integration Tests");

		assertThat(teamscaleMockServer.getSessions()).hasSize(2);
		Session unitTestSession = teamscaleMockServer.getSession("Unit Tests");
		Session integrationTestSession = teamscaleMockServer.getSession("Integration Tests");

		assertThat(integrationTestSession.getRepository()).isEqualTo("myRepoId");
		assertThat(teamscaleMockServer.impactedTestRepositories).containsOnly("myRepoId");

		assertThat(integrationTestSession.getCommit()).matches("abcd1337, .*");
		assertThat(teamscaleMockServer.impactedTestCommits.get(0)).matches("abcd1337, .*");
		assertThat(teamscaleMockServer.impactedTestCommits.get(1)).matches("abcd1337, .*");

		TestwiseCoverageReport unitTestReport = unitTestSession.getOnlyTestwiseCoverageReport();
		assertThat(unitTestReport.partial).isTrue();
		checkExpectedUnitTestCoverage(unitTestReport);

		TestwiseCoverageReport integrationTestReport = integrationTestSession.getOnlyTestwiseCoverageReport();
		checkExpectedIntegrationTestCoverage(integrationTestReport);
	}

	@Test
	public void testPreferBranchAndTimestampOverRevisionWhenProvidedManually() throws IOException {
		SystemTestUtils.runMavenTests("maven-project", "-DteamscaleRevision=abcd1337",
				"-DteamscaleTimestamp=master:HEAD", "-Dtia");

		assertThat(teamscaleMockServer.impactedTestCommits.get(0)).matches("abcd1337, null");
		assertThat(teamscaleMockServer.impactedTestCommits.get(1)).matches("abcd1337, null");
		assertThat(teamscaleMockServer.getSession("Unit Tests").getCommit()).matches("abcd1337, null");
	}

	@Test
	public void testBaselineRevisionIsPreferred() throws IOException {
		SystemTestUtils.runMavenTests("maven-project", "-DbaselineRevision=rev1", "-DbaselineCommit=master:1234",
				"-Dtia");

		assertThat(teamscaleMockServer.baselines).containsOnly("rev1, null");
	}

	@Test
	public void testBaselineCommitIsUsed() throws IOException {
		SystemTestUtils.runMavenTests("maven-project", "-DbaselineCommit=master:1234", "-Dtia");

		assertThat(teamscaleMockServer.baselines).containsOnly("null, master:1234");
	}

	/**
	 * Starts a maven process with the reuseForks flag set to "false". Checks if the coverage
	 * can be converted to a testwise coverage report afterward.
	 */
	@Test
	public void testMavenTiaWithoutReuseForks() throws Exception {
		SystemTestUtils.runMavenTests("maven-project", "-Dtia", "-DreuseForks=false");
		File workingDirectory = new File("maven-project");
		File testwiseCoverage = new File(
				Paths.get(workingDirectory.getAbsolutePath(), "coverage", "target", "tia", "reports",
								"testwise-coverage-1.json")
						.toUri());
		TestwiseCoverageReport testwiseCoverageReport = JsonUtils
				.deserialize(FileSystemUtils.readFile(testwiseCoverage), TestwiseCoverageReport.class);
		checkExpectedUnitTestCoverage(testwiseCoverageReport);
	}

	private static void checkExpectedUnitTestCoverage(TestwiseCoverageReport testwiseCoverageReport) {
		assertNotNull(testwiseCoverageReport);
		assertThat(testwiseCoverageReport.tests).extracting(test -> test.uniformPath).containsExactlyInAnyOrder(
				"org/example/UnitTest/utBla()",
				"org/example/UnitTest/utFoo()",
				"org/example/UnitB1Test/utBlub()",
				"org/example/UnitB1Test/utGoo()",
				"org/example/UnitB2Test/utBlub()",
				"org/example/UnitB2Test/utGoo()"
		);
		assertThat(testwiseCoverageReport.tests).extracting(test -> test.result)
				.allMatch(result -> result == ETestExecutionResult.PASSED);
		assertThat(testwiseCoverageReport.tests).extracting(SystemTestUtils::getCoverageString).containsExactly(
				"SUTA.java:7,10-11;UnitTest.java:10-11",
				"SUTA.java:7,14-15;UnitTest.java:15-16",
				"SUTB1.java:7,14-15;UnitB1Test.java:10-11",
				"SUTB1.java:7,10-11;UnitB1Test.java:15-16",
				// Ensure that sources from the project itself (SUT2), but also other dependent projects is considered (SUT1)
				"SUTB1.java:7,14-15;SUTB2.java:7,14-15;UnitB2Test.java:10-12",
				"SUTB1.java:7,10-11;SUTB2.java:7,10-11;UnitB2Test.java:16-18"
		);
	}

	private static void checkExpectedIntegrationTestCoverage(TestwiseCoverageReport integrationTestReport) {
		assertThat(integrationTestReport.tests).extracting(test -> test.uniformPath)
				.containsExactlyInAnyOrder(
						"org/example/IntegrationIT/itBla()",
						"org/example/IntegrationIT/itFoo()",
						"org/example/IntegrationB1IT/itBlub()",
						"org/example/IntegrationB1IT/itGoo()",
						"org/example/IntegrationB2IT/itBlub()",
						"org/example/IntegrationB2IT/itGoo()"
				);
		assertThat(integrationTestReport.tests).extracting(test -> test.result)
				.allMatch(result -> result == ETestExecutionResult.PASSED);
		assertThat(integrationTestReport.tests).extracting(SystemTestUtils::getCoverageString)
				.containsExactly("IntegrationIT.java:10-11;SUTA.java:7,10-11",
						"IntegrationIT.java:15-16;SUTA.java:7,14-15",
						"IntegrationB1IT.java:10-11;SUTB1.java:7,14-15",
						"IntegrationB1IT.java:15-16;SUTB1.java:7,10-11",
						"IntegrationB2IT.java:10-11;SUTB2.java:7,14-15",
						"IntegrationB2IT.java:15-16;SUTB2.java:7,10-11");
	}
}
