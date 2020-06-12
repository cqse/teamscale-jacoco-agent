package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;

/**
 * Use this class to report test start and end events and upload testwise coverage to Teamscale.
 * <p>
 * After having run all tests, call {@link #endTestRun()} to create a testwise coverage report and upload it to
 * Teamscale. This requires that you configured the agent to upload coverage to Teamscale
 * (`tia-mode=teamscale-upload`).
 */
public class TestRun {

	private final ITestwiseCoverageAgentApi api;

	TestRun(ITestwiseCoverageAgentApi api) {
		this.api = api;
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
	 * Informs the testwise coverage agent that the caller has finished running all tests and should upload coverage to
	 * Teamscale. Only call this if you configured the agent to upload coverage to Teamscale
	 * (`tia-mode=teamscale-upload`). Otherwise, this method will throw an exception.
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
