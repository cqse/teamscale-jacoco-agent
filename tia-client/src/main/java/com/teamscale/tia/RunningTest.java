package com.teamscale.tia;

import com.teamscale.client.PrioritizableTest;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;

import java.io.PrintWriter;
import java.io.StringWriter;

public class RunningTest {

	private final PrioritizableTest test;
	private final ITestwiseCoverageAgentApi api;
	private final long startTime = System.currentTimeMillis();

	public RunningTest(PrioritizableTest test, ITestwiseCoverageAgentApi api) {
		this.test = test;
		this.api = api;
	}

	public void endTestWithException(Throwable throwable) throws AgentHttpRequestFailedException {
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

	public void endTestNormally(TestRun.TestResultWithMessage result) throws AgentHttpRequestFailedException {
		long endTime = System.currentTimeMillis();
		TestExecution execution = new TestExecution(test.uniformPath, endTime - startTime, result.result,
				result.message);
		AgentCommunicationUtils.handleRequestError(api.testFinished(test.uniformPath, execution),
				"Failed to end coverage recording for test case " + test.uniformPath +
						". Coverage for that test case is most likely lost.");
	}

}
