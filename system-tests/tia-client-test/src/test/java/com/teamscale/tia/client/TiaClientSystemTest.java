package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import com.teamscale.test.commons.SystemTestUtils;
import com.teamscale.test.commons.TeamscaleMockServer;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systemundertest.SystemUnderTest;
import testframework.CustomTestFramework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Runs the custom test framework from src/main with our agent attached (the agent is configured in this project's
 * build.gradle). The custom test framework contains an integration via the tia-client against the
 * {@link TeamscaleMockServer}. Asserts that the resulting report looks as expected.
 * <p>
 * This ensures that our tia-client library doesn't break unexpectedly.
 */
public class TiaClientSystemTest {

	/** These ports must match what is configured for the -javaagent line in this project's build.gradle. */
	private static final int FAKE_TEAMSCALE_PORT = 63500;
	private static final int AGENT_PORT = 63501;

	private static TeamscaleMockServer teamscaleMockServer = null;

	@BeforeEach
	public void startFakeTeamscaleServer() throws Exception {
		if (teamscaleMockServer == null) {
			teamscaleMockServer = new TeamscaleMockServer(FAKE_TEAMSCALE_PORT, false, "testFoo", "testBar");
		}
		teamscaleMockServer.uploadedReports.clear();
	}

	@Test
	public void systemTest() throws Exception {
		CustomTestFramework customTestFramework = new CustomTestFramework(AGENT_PORT);
		customTestFramework.runTestsWithTia();

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);

		TestwiseCoverageReport report = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0);
		assertThat(report.tests).hasSize(2);
		assertAll(() -> {
			assertThat(report.tests).extracting(test -> test.uniformPath)
					.containsExactlyInAnyOrder("testBar", "testFoo");
			assertThat(report.tests).extracting(test -> test.result)
					.containsExactlyInAnyOrder(ETestExecutionResult.FAILURE, ETestExecutionResult.PASSED);
			assertThat(report.tests).extracting(SystemTestUtils::getCoverageString)
					.containsExactlyInAnyOrder("SystemUnderTest.java:4,13", "SystemUnderTest.java:4,8");
		});
	}

	@Test
	public void systemTestWithSpecialCharacter() throws Exception {
		TiaAgent agent = new TiaAgent(false, HttpUrl.get("http://localhost:" + AGENT_PORT));
		TestRun testRun = agent.startTestRunWithoutTestSelection();

		String uniformPath = "my/strange;te st[(-+path!@#$%^&*(name";
		RunningTest runningTest = testRun.startTest(uniformPath);
		new SystemUnderTest().foo();
		runningTest.endTest(new TestRun.TestResultWithMessage(ETestExecutionResult.PASSED, ""));

		testRun.endTestRun(true);

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1);

		TestwiseCoverageReport report = teamscaleMockServer.parseUploadedTestwiseCoverageReport(0);
		assertThat(report.tests).hasSize(1);
		assertThat(report.tests).element(0).extracting(test -> test.uniformPath).isEqualTo(uniformPath);
	}

	@AfterAll
	public static void stopFakeTeamscaleServer() {
		teamscaleMockServer.shutdown();
	}
}
