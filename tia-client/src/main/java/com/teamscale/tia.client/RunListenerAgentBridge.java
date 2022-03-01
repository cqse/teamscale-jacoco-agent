package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import okhttp3.HttpUrl;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles communication with the {@link TiaAgent} and logging for any type of test run listener. This allows e.g. Junit
 * 4 and Junit 5 listeners to share the same logic for these tasks.
 */
public class RunListenerAgentBridge {

	private final TestRun testRun;
	private RunningTest runningTest;
	private final Logger logger = LoggerFactory.getLogger(RunListenerAgentBridge.class);

	private static class RunListenerConfigurationException extends RuntimeException {
		public RunListenerConfigurationException(String message) {
			super(message);
		}
	}

	public RunListenerAgentBridge(String runListenerClassName) {
		logger.debug(runListenerClassName + " instantiated");
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
		testRun = agent.startTestRunWithoutTestSelection();
	}

	@FunctionalInterface
	private interface Action {
		/** Runs the action, throwing exceptions if it fails. */
		void run() throws Exception;
	}

	/**
	 * We mustn't throw exceptions out of the {@link RunListener} interface methods or Maven will treat the test as
	 * failed. And we don't have access to the Maven logger, so we just log to stderr.
	 */
	private void handleErrors(Action action, String description) {
		try {
			action.run();
		} catch (Exception e) {
			logger.error("Encountered an error while recording test-wise coverage in step: " + description, e);
		}
	}

	/** Notifies the {@link TiaAgent} that the given test was started. */
	public void testStarted(String uniformPath) {
		logger.debug("Started test '{}'", uniformPath);
		handleErrors(() -> runningTest = testRun.startTest(uniformPath), "Starting test '" + uniformPath + "'");
	}

	/** Notifies the {@link TiaAgent} that the given test was finished (both successfully and unsuccessfully). */
	public void testFinished(String uniformPath, ETestExecutionResult result) {
		testFinished(uniformPath, result, null);
	}

	/**
	 * Notifies the {@link TiaAgent} that the given test was finished (both successfully and unsuccessfully).
	 *
	 * @param message mey be null if no useful message can be provided.
	 */
	public void testFinished(String uniformPath, ETestExecutionResult result, String message) {
		logger.debug("Finished test '{}'", uniformPath);
		handleErrors(() -> {
			if (runningTest != null) {
				runningTest.endTest(new TestRun.TestResultWithMessage(result, message));
				runningTest = null;
			}
		}, "Finishing test '" + uniformPath + "'");
	}

	/**
	 * Notifies the {@link TiaAgent} that the given test was skipped.
	 *
	 * @param reason Optional reason. Pass null if no reason was provided by the test framework.
	 */
	public void testSkipped(String uniformPath, String reason) {
		logger.debug("Skipped test '{}'", uniformPath);
		handleErrors(() -> {
			if (runningTest != null) {
				runningTest.endTest(new TestRun.TestResultWithMessage(ETestExecutionResult.SKIPPED, reason));
				runningTest = null;
			}
		}, "Skipping test '" + uniformPath + "'");
	}

	/**
	 * Notifies the {@link TiaAgent} that the whole test run is finished and that test-wise coverage recording can end
	 * now.
	 */
	public void testRunFinished() {
		logger.debug("Finished test run");
		handleErrors(testRun::endTestRun, "Finishing the test run");
	}
}
