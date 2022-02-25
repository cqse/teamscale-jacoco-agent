package com.teamscale.tia.client;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import okhttp3.HttpUrl;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * {@link TestExecutionListener} that uses the {@link TiaAgent} to record test-wise coverage.
 */
@SuppressWarnings("unused")
public class JUnit5TestWiseCoverageExecutionListener implements TestExecutionListener {

	private final TestRun testRun;
	private RunningTest runningTest;
	private final Logger logger = LoggerFactory.getLogger(JUnit5TestWiseCoverageExecutionListener.class);

	private static class RunListenerConfigurationException extends RuntimeException {
		public RunListenerConfigurationException(String message) {
			super(message);
		}
	}

	public JUnit5TestWiseCoverageExecutionListener() {
		logger.debug(getClass().getName() + " instantiated");
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

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (!testIdentifier.isTest()) {
			return;
		}
		String uniformPath = getUniformPath(testIdentifier);
		logger.debug("Started test '{}'", uniformPath);
		handleErrors(() -> runningTest = testRun.startTest(uniformPath), "Starting test '" + uniformPath + "'");
	}

	private String getUniformPath(TestIdentifier testIdentifier) {
		return testIdentifier.getSource().flatMap(this::parseTestSource).orElse(testIdentifier.getDisplayName());
	}

	private Optional<String> parseTestSource(TestSource source) {
		if (source instanceof ClassSource) {
			ClassSource classSource = (ClassSource) source;
			return Optional.of(classSource.getClassName().replace('.', '/'));
		} else if (source instanceof MethodSource) {
			MethodSource methodSource = (MethodSource) source;
			return Optional.of(methodSource.getClassName().replace('.', '/') + "/" +
					methodSource.getMethodName() + "(" + methodSource.getMethodParameterTypes() + ")");
		}
		return Optional.empty();
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (!testIdentifier.isTest()) {
			return;
		}
		String uniformPath = getUniformPath(testIdentifier);
		logger.debug("Finished test '{}'", uniformPath);
		handleErrors(() -> {
			if (runningTest != null) {
				ETestExecutionResult result;
				switch (testExecutionResult.getStatus()) {
					case SUCCESSFUL:
						result = ETestExecutionResult.PASSED;
						break;
					case ABORTED:
						result = ETestExecutionResult.ERROR;
						break;
					case FAILED:
					default:
						result = ETestExecutionResult.FAILURE;
						break;
				}
				runningTest.endTest(new TestRun.TestResultWithMessage(result, null));
				runningTest = null;
			}
		}, "Finishing test '" + uniformPath + "'");
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (testIdentifier.isContainer()) {
			return;
		}
		String uniformPath = getUniformPath(testIdentifier);
		logger.debug("Skipped test '{}'", uniformPath);
		handleErrors(() -> {
			if (runningTest != null) {
				runningTest.endTest(new TestRun.TestResultWithMessage(ETestExecutionResult.SKIPPED, null));
				runningTest = null;
			}
		}, "Skipping test '" + uniformPath + "'");
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		logger.debug("Finished test run");
		handleErrors(testRun::endTestRun, "Finishing the test run");
	}
}
