package com.teamscale.tia;

import com.teamscale.client.PrioritizableTest;
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

	private final PrioritizableTest test;
	private final ITestwiseCoverageAgentApi api;
	private final long startTime = System.currentTimeMillis();

	public RunningTest(PrioritizableTest test, ITestwiseCoverageAgentApi api) {
		this.test = test;
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
		long endTime = System.currentTimeMillis();

		StringWriter writer = new StringWriter();
		try (PrintWriter printWriter = new PrintWriter(writer)) {
			throwable.printStackTrace(printWriter);
		}
		TestExecution execution = new TestExecution(test.uniformPath, endTime - startTime,
				ETestExecutionResult.ERROR, throwable.getMessage() + "\n" + writer.toString());

		AgentCommunicationUtils.handleRequestError(api.testFinished(test.uniformPath, execution),
				"Failed to end coverage recording for test case " + test.uniformPath +
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
		long endTime = System.currentTimeMillis();
		TestExecution execution = new TestExecution(test.uniformPath, endTime - startTime, result.result,
				result.message);
		AgentCommunicationUtils.handleRequestError(api.testFinished(test.uniformPath, execution),
				"Failed to end coverage recording for test case " + test.uniformPath +
						". Coverage for that test case is most likely lost.");
	}

}
