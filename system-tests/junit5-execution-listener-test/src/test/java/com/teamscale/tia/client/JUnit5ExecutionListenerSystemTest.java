package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.TeamscaleMockServer;
import org.conqat.lib.commons.io.ProcessUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Runs a Maven project's Surefire tests (written with JUnit 5) that have the agent attached and the JUnit 5 {@link
 * org.junit.platform.launcher.TestExecutionListener} enabled. Checks that this produces a correct coverage report.
 */
public class JUnit5ExecutionListenerSystemTest {

	/**
	 * This port must match what is configured for the -javaagent line in the corresponding POM of the Maven test
	 * project.
	 */
	private static final int FAKE_TEAMSCALE_PORT = 65432;

	@Test
	public void systemTest() throws Exception {
		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(FAKE_TEAMSCALE_PORT);

		runMavenTests();

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);

		TestwiseCoverageReport report = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0);
		assertThat(report.tests).hasSize(2);
		assertAll(() -> {
			assertThat(report.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder("JUnit4ExecutedWithJUnit5Test/testAdd()", "JUnit5Test/testAdd()");
			assertThat(report.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.PASSED, ETestExecutionResult.PASSED);
			assertThat(report.tests).extracting(JUnit5ExecutionListenerSystemTest::getCoverageString)
					.containsExactly("SystemUnderTest.java:3,6", "SystemUnderTest.java:3,6");
		});
	}

	private static void runMavenTests() throws IOException {
		ProcessUtils.ExecutionResult result = ProcessUtils.execute(
				new ProcessBuilder("mvnw", "clean", "test").directory(new File("./maven-project")));
		if (!result.isNormalTermination()) {
			throw new IOException("Running Maven failed: " + result.getStdout() + "\n" + result.getStderr());
		}
	}

	private static String getCoverageString(TestInfo info) {
		return info.paths.stream().flatMap(path -> path.getFiles().stream())
				.map(file -> file.fileName + ":" + file.coveredLines).collect(
						Collectors.joining(";"));
	}

}
