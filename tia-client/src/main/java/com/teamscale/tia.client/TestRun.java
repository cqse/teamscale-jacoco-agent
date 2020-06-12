package com.teamscale.tia.client;

import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.ETestExecutionResult;

import java.util.List;

/**
 * Represents a run of prioritized and selected tests as reported by the TIA. Use this class to report test start and
 * end events and upload testwise coverage to Teamscale.
 * <p>
 * The caller of this class should retrieve the tests to execute from {@link #getPrioritizedTests()}, run them (in the
 * given order if possible) and report test start and end events via {@link #startTest(String)} )}.
 * <p>
 * After having run all tests, call {@link #endTestRun()} to create a testwise coverage report and upload it to
 * Teamscale.
 */
public class TestRun {

	private final ITestwiseCoverageAgentApi api;
	private final List<PrioritizableTestCluster> prioritizedTests;

	TestRun(ITestwiseCoverageAgentApi api,
			List<PrioritizableTestCluster> prioritizedTests) {
		this.api = api;
		this.prioritizedTests = prioritizedTests;
	}

	public List<PrioritizableTestCluster> getPrioritizedTests() {
		if (prioritizedTests == null) {
			throw new IllegalStateException("You did not ask the TiaAgent to fetch prioritized tests. Please provide" +
					" test details to TiaAgent#startTestRun()");
		}
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
	 * Informs the testwise coverage agent that a new test is about to start.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. In
	 *                                         this case, the agent probably doesn't know that this test case was
	 *                                         started, so its coverage is lost. This method already retries the request
	 *                                         once, so this is likely a terminal failure. The caller should log this
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
	 * Informs the testwise coverage agent that the caller has finished running all tests and should upload coverage
	 * to Teamscale. Only call this if you configured the agent to upload coverage to Teamscale
	 * (`tia-mode=teamscale-upload`). Otherwise, this method will fail.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The recorded coverage is likely lost. The caller should log this
	 *                                         problem appropriately.
	 */
	public void endTestRun() throws AgentHttpRequestFailedException {
		AgentCommunicationUtils.handleRequestError(api::testRunFinished,
				"Failed to create a coverage report and upload it to Teamscale. The coverage is most likely lost");
	}

}
