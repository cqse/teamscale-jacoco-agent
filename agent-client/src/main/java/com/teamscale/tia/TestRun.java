package com.teamscale.tia;

import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.ETestExecutionResult;

import java.util.List;

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

	@FunctionalInterface
	public interface TestRunner {
		TestResultWithMessage run(PrioritizableTest test) throws Exception;
	}

	public static class TestResultWithMessage {
		public final ETestExecutionResult result;
		public final String message;

		public TestResultWithMessage(ETestExecutionResult result, String message) {
			this.result = result;
			this.message = message;
		}
	}

	public RunningTest startTest(PrioritizableTest test) throws AgentHttpRequestFailedException {
		AgentCommunicationUtils.handleRequestError(api.testStarted(test.uniformPath),
				"Failed to start coverage recording for test case " + test.uniformPath);
		return new RunningTest(test, api);
	}

	public void endTestRun() throws AgentHttpRequestFailedException {
		AgentCommunicationUtils.handleRequestError(api.testRunFinished(),
				"Failed to create a coverage report and upload it to Teamscale. The coverage is most likely lost");
	}

}
