package com.teamscale.test_impacted.engine.executor;

import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.commons.LoggerUtils;
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils.isRepresentative;

/**
 * An execution listener which delegates events to another {@link EngineExecutionListener} and notifies Teamscale agents
 * collecting test wise coverage.
 */
public class TestwiseCoverageCollectingExecutionListener implements EngineExecutionListener {

	private static final Logger LOGGER = LoggerUtils.getLogger(TestwiseCoverageCollectingExecutionListener.class);

	/** An API to signal test start and end to the agent. */
	private final TeamscaleAgentNotifier teamscaleAgentNotifier;

	/** List of tests that have been executed, skipped or failed. */
	private final List<TestExecution> testExecutions = new ArrayList<>();

	/** Time when the current test execution started. */
	private long executionStartTime;

	private final ITestDescriptorResolver testDescriptorResolver;

	private final EngineExecutionListener delegateEngineExecutionListener;

	private final Map<UniqueId, List<TestExecutionResult>> testResultCache = new HashMap<>();

	public TestwiseCoverageCollectingExecutionListener(TeamscaleAgentNotifier teamscaleAgentNotifier,
													   ITestDescriptorResolver testDescriptorResolver,
													   EngineExecutionListener engineExecutionListener) {
		this.teamscaleAgentNotifier = teamscaleAgentNotifier;
		this.testDescriptorResolver = testDescriptorResolver;
		this.delegateEngineExecutionListener = engineExecutionListener;
	}

	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		delegateEngineExecutionListener.dynamicTestRegistered(testDescriptor);
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		if (!TestDescriptorUtils.isRepresentative(testDescriptor)) {
			delegateEngineExecutionListener.executionStarted(testDescriptor);
			testDescriptor.getChildren().forEach(child -> executionSkipped(child, reason));
			delegateEngineExecutionListener.executionFinished(testDescriptor, TestExecutionResult.successful());
			return;
		}

		testDescriptorResolver.getUniformPath(testDescriptor).ifPresent(testUniformPath -> {
			testExecutions.add(new TestExecution(testUniformPath, 0L, ETestExecutionResult.SKIPPED, reason));
			delegateEngineExecutionListener.executionSkipped(testDescriptor, reason);
		});
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		if (isRepresentative(testDescriptor)) {
			testDescriptorResolver.getUniformPath(testDescriptor).ifPresent(teamscaleAgentNotifier::startTest);
			executionStartTime = System.currentTimeMillis();
		}
		delegateEngineExecutionListener.executionStarted(testDescriptor);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		if (isRepresentative(testDescriptor)) {
			Optional<String> uniformPath = testDescriptorResolver.getUniformPath(testDescriptor);
			if (!uniformPath.isPresent()) {
				return;
			}

			TestExecution testExecution = getTestExecution(testDescriptor, testExecutionResult,
					uniformPath.get());
			if (testExecution != null) {
				testExecutions.add(testExecution);
			}
			teamscaleAgentNotifier.endTest(uniformPath.get(), testExecution);
		} else if (testDescriptor.getParent().isPresent()) {
			List<TestExecutionResult> testExecutionResults = testResultCache.computeIfAbsent(
					testDescriptor.getParent().get().getUniqueId(), (key) -> new ArrayList<>());
			testExecutionResults.add(testExecutionResult);
		}

		delegateEngineExecutionListener.executionFinished(testDescriptor, testExecutionResult);
	}

	private TestExecution getTestExecution(TestDescriptor testDescriptor,
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
		List<TestExecutionResult> childTestExecutionResult = testResultCache.remove(testDescriptor.getUniqueId());
		if (childTestExecutionResult != null) {
			testExecutionResults.addAll(childTestExecutionResult);
		}
		testExecutionResults.add(testExecutionResult);
		return testExecutionResults;
	}

	private TestExecution buildTestExecution(String testUniformPath, long duration,
											 Status status, String message) {
		switch (status) {
			case SUCCESSFUL:
				return new TestExecution(testUniformPath, duration, ETestExecutionResult.PASSED);
			case ABORTED:
				return new TestExecution(testUniformPath, duration, ETestExecutionResult.ERROR,
						message);
			case FAILED:
				return new TestExecution(testUniformPath, duration, ETestExecutionResult.FAILURE,
						message);
			default:
				LOGGER.severe(() -> "Got unexpected test execution result status: " + status);
				return null;
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
	public List<TestExecution> getTestExecutions() {
		return testExecutions;
	}
}
