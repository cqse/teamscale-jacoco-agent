package com.teamscale.testimpacted.junit;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.testimpacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.testimpacted.test_descriptor.ITestDescriptorResolver;
import com.teamscale.testimpacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class DelegatingExecutionListener implements EngineExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingExecutionListener.class);

	/** An API service to signal test start and end to the agent. */
	private List<ITestwiseCoverageAgentApi> apiServices;

	/** List of tests that have been executed, skipped or failed. */
	private final List<TestExecution> testExecutions = new ArrayList<>();

	/** Time when the current test execution started. */
	private long executionStartTime;

	private final ITestDescriptorResolver testDescriptorResolver;

	private final EngineExecutionListener delegateEngineExecutionListener;

	DelegatingExecutionListener(List<ITestwiseCoverageAgentApi> apiServices, ITestDescriptorResolver testDescriptorResolver, EngineExecutionListener engineExecutionListener) {
		this.apiServices = apiServices;
		this.testDescriptorResolver = testDescriptorResolver;
		this.delegateEngineExecutionListener = engineExecutionListener;
	}

	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		delegateEngineExecutionListener.dynamicTestRegistered(testDescriptor);
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		List<TestDescriptor> testDescriptors = TestDescriptorUtils.streamLeafTestDescriptors(testDescriptor)
				.collect(Collectors.toList());
		for (TestDescriptor leafTestDescriptor : testDescriptors) {
			Optional<String> testUniformPath = testDescriptorResolver.toUniformPath(leafTestDescriptor);
			if (!testUniformPath.isPresent()) {
				continue;
			}
			testExecutions.add(new TestExecution(testUniformPath.get(), 0, ETestExecutionResult.SKIPPED, reason));
		}
		delegateEngineExecutionListener.executionSkipped(testDescriptor, reason);
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		if (isRelevantTestInstance(testDescriptor)) {
			Optional<String> testUniformPath = testDescriptorResolver.toUniformPath(testDescriptor);
			if (testUniformPath.isPresent()) {
				try {
					for (ITestwiseCoverageAgentApi apiService : apiServices) {
						apiService.testStarted(testUniformPath.get()).execute();
					}
				} catch (IOException e) {
					LOGGER.error(e, () -> "Error while calling service api.");
				}
				executionStartTime = System.currentTimeMillis();
			}
		}
		delegateEngineExecutionListener.executionStarted(testDescriptor);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		if (isRelevantTestInstance(testDescriptor)) {
			Optional<String> testUniformPath = testDescriptorResolver.toUniformPath(testDescriptor);
			if (testUniformPath.isPresent()) {
				try {
					for (ITestwiseCoverageAgentApi apiService : apiServices) {
						apiService.testFinished(testUniformPath.get()).execute();
					}
				} catch (IOException e) {
					LOGGER.error(e, () -> "Error contacting test wise coverage agent.");
				}

				long executionEndTime = System.currentTimeMillis();
				long duration = executionEndTime - executionStartTime;
				String message = getStacktrace(testExecutionResult.getThrowable());
				switch (testExecutionResult.getStatus()) {
					case SUCCESSFUL:
						testExecutions
								.add(new TestExecution(testUniformPath.get(), duration, ETestExecutionResult.PASSED));
						break;
					case ABORTED:
						testExecutions
								.add(new TestExecution(testUniformPath.get(), duration, ETestExecutionResult.ERROR,
										message));
						break;
					case FAILED:
						testExecutions
								.add(new TestExecution(testUniformPath.get(), duration, ETestExecutionResult.FAILURE,
										message));
						break;
				}
			}
		}

		delegateEngineExecutionListener.executionFinished(testDescriptor, testExecutionResult);
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


	private boolean isRelevantTestInstance(TestDescriptor testIdentifier) {
		boolean isParameterizedTestContainer = testIdentifier.isContainer() && containsParameterizedTestContainer(
				testIdentifier);
		boolean isNonParameterizedTest = testIdentifier.isTest() && !containsParameterizedTestContainer(testIdentifier);
		return isNonParameterizedTest || isParameterizedTestContainer;
	}

	/**
	 * Looks like this: [engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[test-template:withValueSource(java.lang.String)]
	 */
	private boolean containsParameterizedTestContainer(TestDescriptor testIdentifier) {
		// TODO (Match segments).
		return testIdentifier.getUniqueId().toString().matches(".*/\\[test-template:[^]]*].*");
	}


	@Override
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		delegateEngineExecutionListener.reportingEntryPublished(testDescriptor, entry);
	}

	/** @see #testExecutions */
	public List<TestExecution> getTestExecutions() {
		return testExecutions;
	}
}
