package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Represents a single test that is currently being executed by the caller of this library. Use {@link
 * #endTestSuccessfully(String)} or {@link #endTestWithException(Throwable)} to signal that executing the test case has
 * finished and test-wise coverage for this test should be stored.
 */
public class RunningTest {

	private final String uniformPath;
	private final ITestwiseCoverageAgentApi api;

	public RunningTest(String uniformPath, ITestwiseCoverageAgentApi api) {
		this.uniformPath = uniformPath;
		this.api = api;
	}

	/**
	 * Signals to the agent that the test failed with the given exception. Displays the exception message and stack
	 * trace in Teamscale.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The caller should record this problem appropriately. Coverage
	 *                                         for subsequent test cases could, however, potentially still be recorded.
	 *                                         Thus, the caller should continue with test execution and continue
	 *                                         informing the coverage agent about further test start and end events.
	 */
	public void endTestWithException(Throwable throwable) throws AgentHttpRequestFailedException {
		StringWriter writer = new StringWriter();
		try (PrintWriter printWriter = new PrintWriter(writer)) {
			throwable.printStackTrace(printWriter);
		}

		// the agent already records test duration, so we can simply provide a dummy value here
		TestExecution execution = new TestExecution(uniformPath, 0L,
				ETestExecutionResult.FAILURE, throwable.getMessage() + "\n" + writer.toString());

		AgentCommunicationUtils.handleRequestError(() -> api.testFinished(uniformPath, execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");
	}

	/**
	 * Signals to the agent that the test runner has finished executing this test and that the test was successful.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The caller should record this problem appropriately. Coverage
	 *                                         for subsequent test cases could, however, potentially still be recorded.
	 *                                         Thus, the caller should continue with test execution and continue
	 *                                         informing the coverage agent about further test start and end events.
	 */
	public void endTestSuccessfully() throws AgentHttpRequestFailedException {
		endTestSuccessfully(null);
	}

	/**
	 * Signals to the agent that the test runner has finished executing this test and that the test was successful.
	 *
	 * @param message An optional (may be null) message to display for this test in Teamscale.
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The caller should record this problem appropriately. Coverage
	 *                                         for subsequent test cases could, however, potentially still be recorded.
	 *                                         Thus, the caller should continue with test execution and continue
	 *                                         informing the coverage agent about further test start and end events.
	 */
	public void endTestSuccessfully(String message) throws AgentHttpRequestFailedException {
		if (message == null) {
			message = "";
		}

		// the agent already records test duration, so we can simply provide a dummy value here
		TestExecution execution = new TestExecution(uniformPath, 0L, ETestExecutionResult.PASSED,
				message);
		AgentCommunicationUtils.handleRequestError(() -> api.testFinished(uniformPath, execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");
	}

	/**
	 * Signals to the agent that the test runner has finished executing this test.
	 *
	 * @param result  The result of the test (e.g. passed, failed, ignored, ...)
	 * @param message An optional (may be null) message to display for this test in Teamscale.
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 *                                         method already retries the request once, so this is likely a terminal
	 *                                         failure. The caller should record this problem appropriately. Coverage
	 *                                         for subsequent test cases could, however, potentially still be recorded.
	 *                                         Thus, the caller should continue with test execution and continue
	 *                                         informing the coverage agent about further test start and end events.
	 */
	public void endTest(ETestExecutionResult result, String message) throws AgentHttpRequestFailedException {
		if (message == null) {
			message = "";
		}

		// the agent already records test duration, so we can simply provide a dummy value here
		TestExecution execution = new TestExecution(uniformPath, 0L, result, message);
		AgentCommunicationUtils.handleRequestError(() -> api.testFinished(uniformPath, execution),
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost.");
	}

}
