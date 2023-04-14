package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.teamscale.test_impacted.engine.ImpactedTestEngine.LOGGER;

/** Test executor that only executes impacted tests and collects test wise coverage for the executed tests. */
public class ImpactedTestsExecutor extends TestwiseCoverageCollectingTestExecutor {


	private final ImpactedTestsProvider impactedTestsProvider;

	public ImpactedTestsExecutor(ImpactedTestsProvider impactedTestsProvider) {
		this.impactedTestsProvider = impactedTestsProvider;
	}

	@Override
	public List<TestExecution> execute(TestExecutorRequest executorRequest) {
		AvailableTests availableTestDetails = TestDescriptorUtils
				.getAvailableTests(executorRequest.testEngine, executorRequest.engineTestDescriptor,
						impactedTestsProvider.partition);
		List<PrioritizableTestCluster> testClusters = impactedTestsProvider.getImpactedTestsFromTeamscale(
				availableTestDetails.getTestList());

		if (testClusters == null) {
			LOGGER.fine(() -> "Falling back to execute all!");
			return super.execute(executorRequest);
		}

		EngineExecutionListener executionListener = new AutoSkippingEngineExecutionListener(
				getImpactedTestUniqueIds(availableTestDetails, testClusters),
				executorRequest.engineExecutionListener, executorRequest.engineTestDescriptor);

		List<TestExecution> testExecutions = new ArrayList<>();

		LOGGER.fine(() -> "Re-discovering tests for delegate engine " + executorRequest.testEngine.getId());

		for (PrioritizableTestCluster testCluster : testClusters) {
			Set<UniqueId> uniqueIdsOfTestsToExecute = availableTestDetails.convertToUniqueIds(testCluster.tests);
			TestDescriptor testDescriptor = getTestDescriptor(executorRequest, uniqueIdsOfTestsToExecute);

			// preserve the root node of the test descriptor hierarchy. This should be our teamscale-test-impacted engine
			executorRequest.engineTestDescriptor.getParent().ifPresent(testDescriptor::setParent);

			TestExecutorRequest impactedExecutorRequest = new TestExecutorRequest(executorRequest.testEngine,
					testDescriptor, executionListener, executorRequest.teamscaleAgentNotifier,
					executorRequest.configurationParameters);
			List<TestExecution> testExecutionsForCluster = super.execute(impactedExecutorRequest);

			testExecutions.addAll(testExecutionsForCluster);
		}

		return testExecutions;
	}

	private static TestDescriptor getTestDescriptor(TestExecutorRequest executorRequest,
													Set<UniqueId> uniqueIdsOfTestsToExecute) {
		UniqueIdsDiscoveryRequest engineDiscoveryRequest = new UniqueIdsDiscoveryRequest(
				uniqueIdsOfTestsToExecute, executorRequest.configurationParameters);
		TestDescriptor testDescriptor = executorRequest.testEngine.discover(engineDiscoveryRequest,
				UniqueId.forEngine(executorRequest.testEngine.getId()));
		Set<UniqueId> missingIds = getMissingUniqueIds(testDescriptor, uniqueIdsOfTestsToExecute);
		if (missingIds.size() > 0) {
			LOGGER.warning(
					"Encountered " + missingIds.size() + " test with unstable names! The engine will fallback to executing the whole container. One example is " + missingIds.iterator()
							.next());
			HashSet<UniqueId> uniqueIds = new HashSet<>(uniqueIdsOfTestsToExecute);
			uniqueIds.removeAll(missingIds);
			uniqueIds.addAll(missingIds.stream().map(UniqueId::removeLastSegment).collect(Collectors.toSet()));
			return getTestDescriptor(executorRequest, uniqueIds);
		}
		return testDescriptor;
	}

	private static Set<UniqueId> getMissingUniqueIds(TestDescriptor testDescriptor, Set<UniqueId> expectedUniqueIds) {
		Set<UniqueId> missingUniqueIds = new HashSet<>(expectedUniqueIds);
		testDescriptor.accept(descriptor -> missingUniqueIds.removeIf(id -> descriptor.getUniqueId().hasPrefix(id)));
		return missingUniqueIds;
	}

	private static Set<UniqueId> getImpactedTestUniqueIds(AvailableTests availableTests,
														  List<PrioritizableTestCluster> testClusters) {
		return testClusters.stream()
				.flatMap(testCluster -> availableTests.convertToUniqueIds(testCluster.tests).stream())
				.collect(Collectors.toSet());
	}
}
