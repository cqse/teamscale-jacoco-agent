package eu.cqse.teamscale.test.listeners;

import eu.cqse.teamscale.report.testwise.model.ETestExecutionResult;
import eu.cqse.teamscale.report.testwise.model.TestExecution;
import eu.cqse.teamscale.test.controllers.ITestwiseCoverageAgentApi;
import okhttp3.HttpUrl;
import org.junit.platform.console.Logger;
import org.junit.platform.console.tasks.TestIdentifierUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementation of the {@link TestExecutionListener} interface provided by the JUnit platform. */
public class JUnit5TestListenerExtension implements TestExecutionListener {

	/** The logger. */
	private Logger logger;

	/** Agent Api client to signal test start and end. */
	private ITestwiseCoverageAgentApi controller;

	/** List of tests that have been executed, skipped or failed. */
	private List<TestExecution> testExecutions = new ArrayList<>();

	/** Time when the current test execution started. */
	private long executionStartTime;

	/** Constructor. */
	public JUnit5TestListenerExtension(HttpUrl url, Logger logger) {
		this.controller = ITestwiseCoverageAgentApi.createService(url);
		this.logger = logger;
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (testIdentifier.isTest()) {
			String testUniformPath = TestIdentifierUtils.getTestUniformPath(testIdentifier, logger);
			try {
				controller.testStarted(testUniformPath).execute();
			} catch (IOException e) {
				logger.error(e);
			}
			executionStartTime = System.currentTimeMillis();
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (testIdentifier.isTest()) {
			String testUniformPath = TestIdentifierUtils.getTestUniformPath(testIdentifier, logger);
			try {
				controller.testFinished(testUniformPath).execute();
			} catch (IOException e) {
				logger.error(e);
			}
			long executionEndTime = System.currentTimeMillis();
			long duration = executionEndTime - executionStartTime;
			String message = getMessage(testExecutionResult.getThrowable());
			switch (testExecutionResult.getStatus()) {
				case SUCCESSFUL:
					testExecutions.add(new TestExecution(testUniformPath, duration, ETestExecutionResult.PASSED));
					break;
				case ABORTED:
					testExecutions
							.add(new TestExecution(testUniformPath, duration, ETestExecutionResult.ERROR, message));
					break;
				case FAILED:
					testExecutions
							.add(new TestExecution(testUniformPath, duration, ETestExecutionResult.FAILURE, message));
					break;
			}
		}
	}

	/** Extracts the stacktrace from the given {@link Throwable} into a string or returns null if no throwable is given. */
	private String getMessage(Optional<Throwable> throwable) {
		if (!throwable.isPresent()) {
			return null;
		}

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.get().printStackTrace(pw);
		return sw.toString();
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (testIdentifier.isTest()) {
			String testUniformPath = TestIdentifierUtils.getTestUniformPath(testIdentifier, logger);
			testExecutions.add(new TestExecution(testUniformPath, 0, ETestExecutionResult.SKIPPED, reason));
		}
	}

	/** @see #testExecutions */
	public List<TestExecution> getTestExecutions() {
		return testExecutions;
	}
}
