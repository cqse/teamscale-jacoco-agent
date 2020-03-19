package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import okhttp3.HttpUrl;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * {@link RunListener} that uses the {@link TiaAgent} to record test-wise coverage.
 */
@SuppressWarnings("unused")
public class TestWiseCoverageRunListener extends RunListener {

	private final TestRun testRun;
	private RunningTest runningTest;

	private static class RunListenerConfigurationException extends RuntimeException {
		public RunListenerConfigurationException(String message) {
			super(message);
		}
	}

	public TestWiseCoverageRunListener() {
		String agentUrl = System.getProperty("tia.agent");
		if (agentUrl == null) {
			agentUrl = System.getenv("TIA_AGENT");
		}
		if (agentUrl == null) {
			throw new RunListenerConfigurationException(
					"You did not provide the URL of a Teamscale JaCoCo agent that will record test-wise coverage." +
							" You can configure the URL either as a system property with -Dtia.agent=URL" +
							" or as an environment variable with TIA_AGENT=URL.");
		}

		TiaAgent agent = new TiaAgent(false, HttpUrl.get(agentUrl));
		testRun = agent.startTestRun();
	}

	@Override
	public void testStarted(Description description) throws Exception {
		runningTest = testRun.startTest(getUniformPath(description));
	}

	private String getUniformPath(Description description) {
		String uniformPath = description.getClassName().replace('.', '/');
		if (description.getMethodName() != null) {
			uniformPath += "/" + description.getMethodName();
		}
		return uniformPath;
	}

	@Override
	public void testFinished(Description description) throws Exception {
		if (runningTest != null) {
			runningTest.endTestNormally(new TestRun.TestResultWithMessage(ETestExecutionResult.PASSED, null));
			runningTest = null;
		}
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		if (runningTest != null) {
			runningTest.endTestNormally(
					new TestRun.TestResultWithMessage(ETestExecutionResult.FAILURE, failure.getTrace()));
			runningTest = null;
		}
	}

	@Override
	public void testAssumptionFailure(Failure failure) {
		if (runningTest != null) {
			try {
				runningTest.endTestNormally(
						new TestRun.TestResultWithMessage(ETestExecutionResult.FAILURE, failure.getTrace()));
				runningTest = null;
			} catch (AgentHttpRequestFailedException e) {
				// we need to rethrow as a runtime exception since the interface does not allow exceptions here,
				// unlike the other listener methods
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		testRun.endTestRun();
	}
}
