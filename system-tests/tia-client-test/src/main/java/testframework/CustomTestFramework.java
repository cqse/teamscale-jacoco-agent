package testframework;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.tia.client.AgentHttpRequestFailedException;
import com.teamscale.tia.client.RunningTest;
import com.teamscale.tia.client.TestRun;
import com.teamscale.tia.client.TestRunWithClusteredSuggestions;
import com.teamscale.tia.client.TiaAgent;
import okhttp3.HttpUrl;
import systemundertest.SystemUnderTest;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/** Simulates a custom test framework that doesn't rely on JUnit. */
public class CustomTestFramework {

	private final Map<String, Runnable> allTests = new HashMap<>();
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
		TestRunWithClusteredSuggestions testRun = agent.startTestRun(
				allTests.keySet().stream().map(name -> new ClusteredTestDetails(name, name, null, null, null))
						.collect(toList()));

		for (PrioritizableTestCluster cluster : testRun.prioritizedClusters) {
			for (PrioritizableTest test : cluster.tests) {
				Runnable runnable = allTests.get(test.testName);
				RunningTest runningTest = testRun.startTest(test.testName);
				try {
					runnable.run();
					runningTest.endTest(new TestRun.TestResultWithMessage(ETestExecutionResult.PASSED, ""));
				} catch (Throwable t) {
					runningTest.endTest(
							new TestRun.TestResultWithMessage(ETestExecutionResult.FAILURE, t.getMessage()));
				}
			}
		}

		testRun.endTestRun(true);
	}

}
