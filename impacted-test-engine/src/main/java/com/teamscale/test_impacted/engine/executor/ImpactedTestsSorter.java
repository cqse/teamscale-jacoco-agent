package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.teamscale.test_impacted.engine.ImpactedTestEngine.LOGGER;

/**
 * Test sorter that requests impacted tests from Teamscale and rewrites the {@link TestDescriptor} to take the returned
 * order into account when executing the tests.
 */
public class ImpactedTestsSorter implements ITestSorter {

	private final ImpactedTestsProvider impactedTestsProvider;

	public ImpactedTestsSorter(ImpactedTestsProvider impactedTestsProvider) {
		this.impactedTestsProvider = impactedTestsProvider;
	}

	@Override
	public void selectAndSort(TestDescriptor rootTestDescriptor) {
		AvailableTests availableTests = TestDescriptorUtils
				.getAvailableTests(rootTestDescriptor, impactedTestsProvider.partition);

		List<PrioritizableTestCluster> testClusters = impactedTestsProvider.getImpactedTestsFromTeamscale(
				availableTests.getTestList());

		if (testClusters == null) {
			LOGGER.fine(() -> "Falling back to execute all!");
			return;
		}

		Set<? super TestDescriptor> testRepresentatives = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<? super TestDescriptor> seenDescriptors = Collections.newSetFromMap(new IdentityHashMap<>());
		for (PrioritizableTestCluster testCluster : testClusters) {
			for (PrioritizableTest test : testCluster.tests) {
				Optional<UniqueId> uniqueId = availableTests.convertToUniqueId(test);
				if (!uniqueId.isPresent()) {
					LOGGER.severe(() -> "Falling back to execute all...");
					return;
				}
				Optional<? extends TestDescriptor> testDescriptor = rootTestDescriptor.findByUniqueId(uniqueId.get());
				if (!testDescriptor.isPresent()) {
					LOGGER.severe(() -> "Falling back to execute all...");
					return;
				}
				testRepresentatives.add(testDescriptor.get());
				reinsertIntoHierarchy(testDescriptor.get(), seenDescriptors);
			}
		}

		removeNonImpactedTests(rootTestDescriptor, testRepresentatives);
	}

	/**
	 * Reinserts the given testDescriptor into the hierarchy by walking up the parents chain. By doing this in order
	 * with all tests we end up with our intended order. This is continued until we reach a node that has already been
	 * reinserted in a previous run, because parents should be sorted according to the order of their most important
	 * child descriptors.
	 */
	private static void reinsertIntoHierarchy(TestDescriptor testDescriptor,
											  Set<? super TestDescriptor> seenDescriptors) {
		Optional<? extends TestDescriptor> currentTestDescriptor = Optional.of(testDescriptor);
		while (currentTestDescriptor.isPresent() && !seenDescriptors.contains(currentTestDescriptor.get())) {
			seenDescriptors.add(currentTestDescriptor.get());
			TestDescriptorUtils.reinsertIntoParent(currentTestDescriptor.get());
			currentTestDescriptor = currentTestDescriptor.get().getParent();
		}
	}

	private void removeNonImpactedTests(TestDescriptor testDescriptor, Set<? super TestDescriptor> testDescriptors) {
		if (testDescriptors.contains(testDescriptor)) {
			return;
		}
		for (TestDescriptor descriptor : new ArrayList<>(testDescriptor.getChildren())) {
			removeNonImpactedTests(descriptor, testDescriptors);
		}
		if (testDescriptor.getChildren().isEmpty()) {
			testDescriptor.removeFromHierarchy();
		}
	}
}
