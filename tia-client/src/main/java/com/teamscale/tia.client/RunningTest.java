package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Represents a single test that is currently being executed by the caller of this library. Use {@link
 * #endTestNormally(TestRun.TestResultWithMessage)} or {@link #endTestWithTestRunnerException(Throwable)} to signal that
 * executing the test case has finished and test-wise coverage for this test should be stored.
 */
@SuppressWarnings("unused")
public class RunningTest {

	private final String uniformPath;
	private final ITestwiseCoverageAgentApi api;

	public RunningTest(String uniformPath, ITestwiseCoverageAgentApi api) {
		this.uniformPath = uniformPath;
		this.api = api;
	}

	/**
	 * Signals to the agent that an internal error occurred in the test runner while executing this test. E.g. the test
	 * was not executed at all.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The caller should record this problem appropriately. Coverage
	 *                                         for subsequent test cases could, however, potentially still be recorded.
	 *                                         Thus, the caller should continue with test execution and continue
	 *                                         informing the coverage agent about further test start and end events.
	 */
	public void endTestWithTestRunnerException(Throwable throwable) throws AgentHttpRequestFailedException {
		StringWriter writer = new StringWriter();
		try (PrintWriter printWriter = new PrintWriter(writer)) {
			throwable.printStackTrace(printWriter);
		}

		// the agent already records test duration, so we can simply provide a dummy value here
		TestExecution execution = new TestExecution(uniformPath, 0L,
				ETestExecutionResult.ERROR, throwable.getMessage() + "\n" + writer.toString());

		AgentCommunicationUtils.handleRequestError(api.testFinished(uniformPath, execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");
	}

	/**
	 * Signals to the agent that the test runner has finished executing this test and the result of the test run.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The caller should record this problem appropriately. Coverage
	 *                                         for subsequent test cases could, however, potentially still be recorded.
	 *                                         Thus, the caller should continue with test execution and continue
	 *                                         informing the coverage agent about further test start and end events.
	 */
	public void endTestNormally(TestRun.TestResultWithMessage result) throws AgentHttpRequestFailedException {
		// the agent already records test duration, so we can simply provide a dummy value here
		TestExecution execution = new TestExecution(uniformPath, 0L, result.result,
				result.message);
		AgentCommunicationUtils.handleRequestError(api.testFinished(uniformPath, execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");
	}

}
