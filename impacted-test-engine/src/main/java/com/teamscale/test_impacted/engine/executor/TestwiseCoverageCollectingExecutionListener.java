package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import com.teamscale.tia.client.ITestwiseCoverageAgentApi;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils.isTestRepresentative;

/**
 * An execution listener which delegates events to another {@link EngineExecutionListener} and notifies Teamscale agents
 * collecting test wise coverage.
 */
class TestwiseCoverageCollectingExecutionListener implements EngineExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestwiseCoverageCollectingExecutionListener.class);

	/** An API service to signal test start and end to the agent. */
	private List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis;

	/** List of tests that have been executed, skipped or failed. */
	private final List<TestExecution> testExecutions = new ArrayList<>();

	/** Time when the current test execution started. */
	private long executionStartTime;

	private final ITestDescriptorResolver testDescriptorResolver;

	private final EngineExecutionListener delegateEngineExecutionListener;

	private final Map<UniqueId, TestExecutionResult> testResultCache = new HashMap<>();

	TestwiseCoverageCollectingExecutionListener(List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis,
												ITestDescriptorResolver testDescriptorResolver,
												EngineExecutionListener engineExecutionListener) {
		this.testwiseCoverageAgentApis = testwiseCoverageAgentApis;
		this.testDescriptorResolver = testDescriptorResolver;
		this.delegateEngineExecutionListener = engineExecutionListener;
	}

	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		delegateEngineExecutionListener.dynamicTestRegistered(testDescriptor);
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		if (!TestDescriptorUtils.isTestRepresentative(testDescriptor)) {
			delegateEngineExecutionListener.executionStarted(testDescriptor);
			testDescriptor.getChildren().forEach(child -> this.executionSkipped(child, reason));
			delegateEngineExecutionListener.executionFinished(testDescriptor, TestExecutionResult.successful());
			return;
		}

		testDescriptorResolver.getUniformPath(testDescriptor).ifPresent(testUniformPath -> {
			if (!AutoSkippingEngineExecutionListener.TEST_NOT_IMPACTED_REASON.equals(reason)) {
				testExecutions.add(new TestExecution(testUniformPath, 0L, ETestExecutionResult.SKIPPED, reason));
			}
			delegateEngineExecutionListener.executionSkipped(testDescriptor, reason);
		});
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		if (isTestRepresentative(testDescriptor)) {
			testDescriptorResolver.getUniformPath(testDescriptor).ifPresent(this::startTest);
		}
		delegateEngineExecutionListener.executionStarted(testDescriptor);
	}

	private void startTest(String testUniformPath) {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				apiService.testStarted(testUniformPath).execute();
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error while calling service api.");
		}
		executionStartTime = System.currentTimeMillis();
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		if (isTestRepresentative(testDescriptor)) {
			Optional<String> uniformPath = testDescriptorResolver.getUniformPath(testDescriptor);
			if (!uniformPath.isPresent()) {
				return;
			}

			Optional<TestExecution> testExecution = getTestExecution(testDescriptor, testExecutionResult,
					uniformPath.get());
			testExecution.ifPresent(testExecutions::add);
			endTest(uniformPath.get());
		} else {
			testResultCache.put(testDescriptor.getUniqueId(), testExecutionResult);

			if (!testDescriptor.getParent().isPresent()) {
				// this is the root node, i.e. test execution is completely finished now
				endTestRun();
			}
		}

		delegateEngineExecutionListener.executionFinished(testDescriptor, testExecutionResult);
	}

	private Optional<TestExecution> getTestExecution(TestDescriptor testDescriptor,
													 TestExecutionResult testExecutionResult, String testUniformPath) {
		List<TestExecutionResult> testExecutionResults = getTestExecutionResults(testDescriptor, testExecutionResult);

		long executionEndTime = System.currentTimeMillis();
		long duration = executionEndTime - executionStartTime;
		StringBuilder message = new StringBuilder();
		Status status = Status.SUCCESSFUL;
		for (TestExecutionResult executionResult : testExecutionResults) {
			if (message.length() > 0) {
				message.append("\n\n");
			}
			message.append(getStacktrace(executionResult.getThrowable()));
			// Aggregate status here to most severe status according to SUCCESSFUL < ABORTED < FAILED
			if (status.ordinal() < executionResult.getStatus().ordinal()) {
				status = executionResult.getStatus();
			}
		}

		return buildTestExecution(testUniformPath, duration, status, message.toString());
	}

	private List<TestExecutionResult> getTestExecutionResults(TestDescriptor testDescriptor,
															  TestExecutionResult testExecutionResult) {
		List<TestExecutionResult> testExecutionResults = new ArrayList<>();
		for (TestDescriptor child : testDescriptor.getChildren()) {
			TestExecutionResult childTestExecutionResult = testResultCache.remove(child.getUniqueId());
			if (childTestExecutionResult != null) {
				testExecutionResults.add(childTestExecutionResult);
			} else {
				LOGGER.warn(() -> "No test execution found for " + child.getUniqueId());
			}
		}
		testExecutionResults.add(testExecutionResult);
		return testExecutionResults;
	}

	private void endTest(String testUniformPath) {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				apiService.testFinished(testUniformPath).execute();
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error contacting test wise coverage agent.");
		}
	}

	private void endTestRun() {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				apiService.testRunFinished().execute();
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error contacting test wise coverage agent.");
		}
	}


	private Optional<TestExecution> buildTestExecution(String testUniformPath, long duration,
													   Status status, String message) {
		switch (status) {
			case SUCCESSFUL:
				return Optional.of(new TestExecution(testUniformPath, duration, ETestExecutionResult.PASSED));
			case ABORTED:
				return Optional.of(new TestExecution(testUniformPath, duration, ETestExecutionResult.ERROR,
						message));
			case FAILED:
				return Optional.of(new TestExecution(testUniformPath, duration, ETestExecutionResult.FAILURE,
						message));
			default:
				LOGGER.error(() -> "Got unexpected test execution result status: " + status);
				return Optional.empty();
		}
	}

	/** Extracts the stacktrace from the given {@link Throwable} into a string or returns null if no throwable is given. */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		delegateEngineExecutionListener.reportingEntryPublished(testDescriptor, entry);
	}

	/** @see #testExecutions */
	List<TestExecution> getTestExecutions() {
		return testExecutions;
	}
}
