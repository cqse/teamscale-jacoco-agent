package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.reporting.ReportEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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

	TestwiseCoverageCollectingExecutionListener(List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis, ITestDescriptorResolver testDescriptorResolver, EngineExecutionListener engineExecutionListener) {
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
		TestDescriptorUtils.streamTestRepresentatives(testDescriptor).forEach(testRerpesentative -> {
			Optional<String> testUniformPath = testDescriptorResolver.getUniformPath(testRerpesentative);
			if (!testUniformPath.isPresent()) {
				return;
			}
			testExecutions.add(new TestExecution(testUniformPath.get(), 0, ETestExecutionResult.SKIPPED, reason));
		});
		delegateEngineExecutionListener.executionSkipped(testDescriptor, reason);
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
			testDescriptorResolver.getUniformPath(testDescriptor)
					.ifPresent(testUniformPath -> endTest(testExecutionResult, testUniformPath));
		}

		delegateEngineExecutionListener.executionFinished(testDescriptor, testExecutionResult);
	}

	private void endTest(TestExecutionResult testExecutionResult, String testUniformPath) {
		try {
			for (ITestwiseCoverageAgentApi apiService : testwiseCoverageAgentApis) {
				apiService.testFinished(testUniformPath).execute();
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error contacting test wise coverage agent.");
		}

		getTestExecution(testExecutionResult, testUniformPath).ifPresent(testExecutions::add);
	}

	private Optional<TestExecution> getTestExecution(TestExecutionResult testExecutionResult, String testUniformPath) {
		long executionEndTime = System.currentTimeMillis();
		long duration = executionEndTime - executionStartTime;
		String message = getStacktrace(testExecutionResult.getThrowable());
		Status status = testExecutionResult.getStatus();

		switch (status) {
			case SUCCESSFUL:
				return Optional.of(new TestExecution(testUniformPath, duration, ETestExecutionResult.PASSED));
			case ABORTED:
				return Optional.of(new TestExecution(testUniformPath, duration, ETestExecutionResult.ERROR,
						message));
			case FAILED:
				return Optional.of(new TestExecution(testUniformPath, duration, ETestExecutionResult.FAILURE, message));
			default:
				LOGGER.error(() -> "Got unexpected test exectuion result status: " + status);
				return Optional.empty();
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
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		delegateEngineExecutionListener.reportingEntryPublished(testDescriptor, entry);
	}

	/** @see #testExecutions */
	List<TestExecution> getTestExecutions() {
		return testExecutions;
	}
}
