package com.teamscale.test_impacted.engine.executor;

import com.teamscale.test_impacted.engine.ImpactedTestEngine;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@link EngineExecutionListener} which tracks which impacted child {@link TestDescriptor}s of a container {@link
 * TestDescriptor} have already been executed. Will skip remaining non-impacted child {@link TestDescriptor}s once all
 * impacted {@link TestDescriptor} have been finished. Needed to ensure that all children of a container {@link
 * TestDescriptor} have been finished or skipped before the container {@link TestDescriptor} is finished.
 */
class AutoSkippingEngineExecutionListener implements EngineExecutionListener {

	/** Skip reason message if a test case is not impacted. */
	static final String TEST_NOT_IMPACTED_REASON = "Test not impacted by code changes.";

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoSkippingEngineExecutionListener.class);

	/**
	 * All test descriptors for which we have triggered a start event. Needed to ensure that only a single start event
	 * occurs per test descriptor.
	 */
	private final Set<UniqueId> startedTestDescriptorIds = new HashSet<>();

	/**
	 * The not yet finished or skipped impacted {@link TestDescriptor}s. Also contains container {@link TestDescriptor}s
	 * which have impacted {@link TestDescriptor} children.
	 */
	private final Set<UniqueId> openImpactedTestDescriptorIds;

	/**
	 * Finished or skipped impacted {@link TestDescriptor}s. Also contains impacted container {@link TestDescriptor}s
	 * which no longer have impacted {@link TestDescriptor} children.
	 */
	private final Set<UniqueId> finishedImpactedTestDescriptorIds = new HashSet<>();

	/** The delegate {@link EngineExecutionListener} to pass events to. */
	private final EngineExecutionListener delegateExecutionListener;

	/**
	 * Map to resolve the original {@link TestDescriptor} objects which were passed in the original {@link
	 * ExecutionRequest} to the {@link ImpactedTestEngine}. We resolve them here to actually pass back the exact {@link
	 * TestDescriptor}s for which execution was requested instead the re-discovered ones. Note that this map also
	 * contains non-impacted {@link TestDescriptor}s which are auto skipped once all their impacted children are
	 * finished.
	 */
	private final Map<UniqueId, TestDescriptor> requestedTestDescriptorsById = new HashMap<>();

	AutoSkippingEngineExecutionListener(Set<UniqueId> impactedTestDescriptorIds,
										EngineExecutionListener delegateExecutionListener,
										TestDescriptor requestedEngineTestDescriptor) {
		this.openImpactedTestDescriptorIds = new HashSet<>(impactedTestDescriptorIds);
		this.delegateExecutionListener = delegateExecutionListener;
		registerTestDescriptors(requestedEngineTestDescriptor);
	}

	/**
	 * Registers the open impacted {@link TestDescriptor}s at {@link #openImpactedTestDescriptorIds} and the the
	 * requested {@link TestDescriptor}s from the {@link ExecutionRequest} at {@link #requestedTestDescriptorsById}.
	 */
	private void registerTestDescriptors(TestDescriptor testDescriptor) {
		// First register impacted test descriptors from child test descriptors
		for (TestDescriptor testDescriptorChild : testDescriptor.getChildren()) {
			registerTestDescriptors(testDescriptorChild);
		}

		UniqueId uniqueId = testDescriptor.getUniqueId();

		if (openImpactedTestDescriptorIds.contains(uniqueId)) {
			// Since this node is impacted it's parent is also considered impacted. Note that the previous loop ensures
			// that we have added impacted child test descriptors to the open impacted test descriptors first.
			testDescriptor.getParent().map(TestDescriptor::getUniqueId).ifPresent(openImpactedTestDescriptorIds::add);
		}

		requestedTestDescriptorsById.put(uniqueId, testDescriptor);
	}

	private TestDescriptor resolveOriginalTestDescriptor(TestDescriptor testDescriptor) {
		UniqueId testDescriptorUniqueId = testDescriptor.getUniqueId();
		TestDescriptor requestedTestDescriptor = requestedTestDescriptorsById.get(testDescriptorUniqueId);

		if (requestedTestDescriptor != null) {
			return requestedTestDescriptor;
		}

		LOGGER.error(() -> "Encountered test descriptor which isn't dynamically registered and can't be resolved to " +
				"an original test descriptor found during discovery: " + testDescriptor);
		return testDescriptor;
	}

	/**
	 * Finishes an impacted {@link TestDescriptor} by moving it from the {@link #openImpactedTestDescriptorIds} to the
	 * {@link #finishedImpactedTestDescriptorIds}. Also asserts that the move actually occurred. Assumes that all
	 * unfinished child {@link TestDescriptor}s represent not impacted tests and skips them.
	 */
	private void finishTestDescriptor(TestDescriptor testDescriptor) {
		for (TestDescriptor testDescriptorChild : testDescriptor.getChildren()) {
			if (!finishedImpactedTestDescriptorIds.contains(testDescriptorChild.getUniqueId())) {
				delegateExecutionListener.executionSkipped(testDescriptorChild, TEST_NOT_IMPACTED_REASON);
			}
		}
		Preconditions.condition(openImpactedTestDescriptorIds.remove(testDescriptor.getUniqueId()),
				() -> "Expected impacted and unfinished test descriptor to be part of the open impacted nodes: " + testDescriptor);
		Preconditions.condition(finishedImpactedTestDescriptorIds.add(testDescriptor.getUniqueId()),
				() -> "Expected impacted and unfinished test descriptor to not be part of the finished impacted nodes: " + testDescriptor);
	}

	/**
	 * Wraps the dynamicall registered {@link TestDescriptor} into a new one which is part of the parent-child hierarchy
	 * of the requested {@link TestDescriptor}s in {@link #requestedTestDescriptorsById}.
	 */
	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		TestDescriptor wrappedTestDescriptor = new DelegatingTestDescriptor(testDescriptor);
		UniqueId uniqueId = wrappedTestDescriptor.getUniqueId();

		Optional<TestDescriptor> parentTestDescriptor = testDescriptor.getParent();
		Preconditions.condition(parentTestDescriptor.isPresent(),
				"Dynamically registered test with id " + uniqueId + " must have a known parent test descriptor.");

		TestDescriptor originalParentTestDescriptor = requestedTestDescriptorsById
				.get(parentTestDescriptor.get().getUniqueId());

		originalParentTestDescriptor.addChild(wrappedTestDescriptor);
		requestedTestDescriptorsById.put(uniqueId, wrappedTestDescriptor);
		openImpactedTestDescriptorIds.add(uniqueId);
		delegateExecutionListener.dynamicTestRegistered(wrappedTestDescriptor);
	}

	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		TestDescriptor originalTestDescriptor = resolveOriginalTestDescriptor(testDescriptor);
		delegateExecutionListener.executionSkipped(originalTestDescriptor, reason);
		finishTestDescriptor(originalTestDescriptor);
	}

	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		TestDescriptor originalTestDescriptor = resolveOriginalTestDescriptor(testDescriptor);

		if (startedTestDescriptorIds.add(originalTestDescriptor.getUniqueId())) {
			delegateExecutionListener.executionStarted(originalTestDescriptor);
		}
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		TestDescriptor originalTestDescriptor = resolveOriginalTestDescriptor(testDescriptor);

		if (originalTestDescriptor.getChildren().stream().map(TestDescriptor::getUniqueId)
				.anyMatch(openImpactedTestDescriptorIds::contains)) {
			// Still open impacted child test descriptors left. Postpone finish until the last child is finished.
			return;
		}

		finishTestDescriptor(originalTestDescriptor);
		delegateExecutionListener.executionFinished(originalTestDescriptor, testExecutionResult);
	}

	@Override
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		delegateExecutionListener.reportingEntryPublished(resolveOriginalTestDescriptor(testDescriptor), entry);
	}
}
