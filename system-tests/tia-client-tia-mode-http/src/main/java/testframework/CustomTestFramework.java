package testframework;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.tia.client.AgentHttpRequestFailedException;
import com.teamscale.tia.client.RunningTest;
import com.teamscale.tia.client.TestRun;
import com.teamscale.tia.client.TiaAgent;
import okhttp3.HttpUrl;
import systemundertest.SystemUnderTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Simulates a custom test framework that doesn't rely on JUnit. */
public class CustomTestFramework {

	private final Map<String, Runnable> allTests = new HashMap<>();
	public final List<TestInfo> testInfos = new ArrayList<>();
	private final int agentPort;

	public CustomTestFramework(int agentPort) {
		this.agentPort = agentPort;
		allTests.put("testFoo", () -> {
			if (new SystemUnderTest().foo() != 5) {
				throw new AssertionError("Incorrect return value for foo");
			}
		});
		allTests.put("testBar", () -> {
			if (new SystemUnderTest().bar() != 7) {
				throw new AssertionError("Incorrect return value for bar");
			}
		});
	}

	/** Talks to the TIA agent and runs all impacted tests. Also uploads a coverage report at the end. */
	public void runTestsWithTia() throws AgentHttpRequestFailedException {
		TiaAgent agent = new TiaAgent(false, HttpUrl.get("http://localhost:" + agentPort));
		TestRun testRun = agent.startTestRunWithoutTestSelection();

		for (String uniformPath : allTests.keySet()) {
			Runnable runnable = allTests.get(uniformPath);
			RunningTest runningTest = testRun.startTest(uniformPath);

			TestInfo testInfo;
			try {
				runnable.run();
				testInfo = runningTest.endTestAndRetrieveCoverage(
						new TestRun.TestResultWithMessage(ETestExecutionResult.PASSED, ""));
			} catch (Throwable t) {
				testInfo = runningTest.endTestAndRetrieveCoverage(
						new TestRun.TestResultWithMessage(ETestExecutionResult.FAILURE, t.getMessage()));
			}
			testInfos.add(testInfo);
		}
	}

}
