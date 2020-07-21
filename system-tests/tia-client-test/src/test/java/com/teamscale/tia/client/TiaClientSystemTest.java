package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import org.junit.jupiter.api.Test;
import testframework.CustomTestFramework;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Runs the custom test framework from src/main with our agent attached (the agent is configured in this project's
 * build.gradle). The custom test framework contains an integration via the tia-client against the {@link
 * TeamscaleMockServer}. Asserts that the resulting report looks as expected.
 * <p>
 * This ensures that our tia-client library doesn't break unexpectedly.
 */
public class TiaClientSystemTest {

	/** These ports must match what is configured for the -javaagent line in this project's build.gradle. */
	private final int fakeTeamscalePort = 65432;
	private final int agentPort = 65433;

	@Test
	public void systemTest() throws Exception {
		TeamscaleMockServer teamscaleMockServer = new TeamscaleMockServer(fakeTeamscalePort, "testFoo", "testBar");
		CustomTestFramework customTestFramework = new CustomTestFramework(agentPort);
		customTestFramework.runTestsWithTia();

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);

		TestwiseCoverageReport report = teamscaleMockServer.uploadedReports.get(0);
		assertThat(report.tests).hasSize(2);
		assertAll(() -> {
			assertThat(report.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder("testBar", "testFoo");
			assertThat(report.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.FAILURE, ETestExecutionResult.PASSED);
			assertThat(report.tests).extracting(TiaClientSystemTest::getCoverageString)
					.containsExactlyInAnyOrder("SystemUnderTest.java:4,13", "SystemUnderTest.java:4,8");
		});
	}

	private static String getCoverageString(TestInfo info) {
		return info.paths.stream().flatMap(path -> path.getFiles().stream())
				.map(file -> file.fileName + ":" + file.coveredLines).collect(
						Collectors.joining(";"));
	}

}
