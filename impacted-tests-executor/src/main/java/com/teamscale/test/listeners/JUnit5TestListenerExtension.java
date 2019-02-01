package com.teamscale.test.listeners;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test.controllers.ITestwiseCoverageAgentApi;
import okhttp3.HttpUrl;
import org.junit.platform.console.Logger;
import org.junit.platform.console.tasks.TestIdentifierUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/** Implementation of the {@link TestExecutionListener} interface provided by the JUnit platform. */
public class JUnit5TestListenerExtension implements TestExecutionListener {

	/** The logger. */
	private Logger logger;

	/** An API service to signal test start and end to the agent. */
	private List<ITestwiseCoverageAgentApi> apiServices;

	/** List of tests that have been executed, skipped or failed. */
	private final List<TestExecution> testExecutions = new ArrayList<>();

	/** Time when the current test execution started. */
	private long executionStartTime;

	/** The currently executed test plan. */
	private TestPlan testPlan;

	/** Constructor. */
	public JUnit5TestListenerExtension(List<HttpUrl> urls, Logger logger) {
		this.apiServices = urls.stream().map(ITestwiseCoverageAgentApi::createService).collect(toList());
		this.logger = logger;
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		this.testPlan = testPlan;
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		this.testPlan = null;
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (isRelevantTestInstance(testIdentifier)) {
			String testUniformPath = TestIdentifierUtils.getTestUniformPath(testIdentifier, logger);
			try {
				for (ITestwiseCoverageAgentApi apiService : apiServices) {
					apiService.testStarted(testUniformPath).execute();
				}
			} catch (IOException e) {
				logger.error(e);
			}
			executionStartTime = System.currentTimeMillis();
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (isRelevantTestInstance(testIdentifier)) {
			String testUniformPath = TestIdentifierUtils.getTestUniformPath(testIdentifier, logger);
			try {
				for (ITestwiseCoverageAgentApi apiService : apiServices) {
					apiService.testFinished(testUniformPath).execute();
				}
			} catch (IOException e) {
				logger.error(e);
			}
			long executionEndTime = System.currentTimeMillis();
			long duration = executionEndTime - executionStartTime;

			String message = getStacktrace(testExecutionResult.getThrowable());
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
				default:
					logger.error("New unknown execution result: " + testExecutionResult.getStatus());
					break;
			}
		}
	}

	/** Extracts the stacktrace from the given {@link Throwable} into a string or returns null if no throwable is given. */
	private String getStacktrace(Optional<Throwable> throwable) {
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
		List<TestIdentifier> testIdentifiers = collectTestIdentifiers(testIdentifier);
		for (TestIdentifier identifier : testIdentifiers) {
			String testUniformPath = TestIdentifierUtils.getTestUniformPath(identifier, logger);
			testExecutions.add(new TestExecution(testUniformPath, 0, ETestExecutionResult.SKIPPED, reason));
		}
	}

	/** Returns a flattened list of all test leafs in the given test identifier. */
	private List<TestIdentifier> collectTestIdentifiers(TestIdentifier testIdentifier) {
		if (isRelevantTestInstance(testIdentifier)) {
			return Collections.singletonList(testIdentifier);
		}
		return testPlan.getChildren(testIdentifier).stream()
				.flatMap(nestedTestIdentifier -> collectTestIdentifiers(nestedTestIdentifier).stream())
				.collect(toList());
	}

	private boolean isRelevantTestInstance(TestIdentifier testIdentifier) {
		boolean isParameterizedTestContainer = testIdentifier.isContainer() && containsParameterizedTestContainer(
				testIdentifier);
		boolean isNonParameterizedTest = testIdentifier.isTest() && !containsParameterizedTestContainer(testIdentifier);
		return isNonParameterizedTest || isParameterizedTestContainer;
	}

	/**
	 * Looks like this: [engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[test-template:withValueSource(java.lang.String)]
	 */
	private boolean containsParameterizedTestContainer(TestIdentifier testIdentifier) {
		return testIdentifier.getUniqueId().matches(".*/\\[test\\-template:[^\\]]*\\].*");
	}

	/** @see #testExecutions */
	public List<TestExecution> getTestExecutions() {
		return testExecutions;
	}
}
