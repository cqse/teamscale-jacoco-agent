package com.teamscale.tia.client;

import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.ETestExecutionResult;

import java.util.List;

/**
 * Represents a run of prioritized and selected tests as reported by the TIA. Use this class to report test start and
 * end events and upload test-wise coverage to Teamscale.
 * <p>
 * The caller of this class should retrieve the tests to execute from {@link #getPrioritizedTests()}, run them (in the
 * given order if possible) and report test start and end events via {@link #startTest(String)} )}.
 * <p>
 * After having run all tests, call {@link #endTestRun()} to create a test-wise coverage report and upload it to
 * Teamscale.
 */
@SuppressWarnings("unused")
public class TestRun {

	private final ITestwiseCoverageAgentApi api;
	private final List<PrioritizableTestCluster> prioritizedTests;

	TestRun(ITestwiseCoverageAgentApi api,
			List<PrioritizableTestCluster> prioritizedTests) {
		this.api = api;
		this.prioritizedTests = prioritizedTests;
	}

	public List<PrioritizableTestCluster> getPrioritizedTests() {
		return prioritizedTests;
	}

	/**
	 * Represents the result of running a single test.
	 */
	public static class TestResultWithMessage {

		/** Whether the test succeeded or failed. */
		public final ETestExecutionResult result;

		/** An optional message, e.g. a stack trace in case of test failures. */
		public final String message;

		public TestResultWithMessage(ETestExecutionResult result, String message) {
			this.result = result;
			this.message = message;
		}
	}

	/**
	 * Informs the test-wise coverage agent that a new test is about to start.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. In
	 *                                         this case, the agent probably doesn't know that this test case was
	 *                                         started, so its coverage is lost. This method already retries the request
	 *                                         once, so this is likely a terminal failure. The caller should record this
	 *                                         problem appropriately. Coverage for subsequent test cases could, however,
	 *                                         potentially still be recorded. Thus, the caller should continue with test
	 *                                         execution and continue informing the coverage agent about further test
	 *                                         start and end events.
	 */
	public RunningTest startTest(String uniformPath) throws AgentHttpRequestFailedException {
		AgentCommunicationUtils.handleRequestError(() -> api.testStarted(uniformPath),
				"Failed to start coverage recording for test case " + uniformPath);
		return new RunningTest(uniformPath, api);
	}

	/**
	 * Informs the test-wise coverage agent that the caller has finished running all tests and instructs it to upload
	 * the recorded test-wise coverage to Teamscale.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The recorded coverage is likely lost. The caller should record
	 *                                         this problem appropriately.
	 */
	public void endTestRun() throws AgentHttpRequestFailedException {
		AgentCommunicationUtils.handleRequestError(api::testRunFinished,
				"Failed to create a coverage report and upload it to Teamscale. The coverage is most likely lost");
	}

}
