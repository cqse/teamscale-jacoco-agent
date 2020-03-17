package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import com.teamscale.tia.ITestwiseCoverageAgentApi;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Test executor that only executes impacted tests and collects test wise coverage for the executed tests. */
public class ImpactedTestsExecutor extends TestwiseCoverageCollectingTestExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactedTestsExecutor.class);

	private final ImpactedTestsProvider impactedTestsProvider;

	public ImpactedTestsExecutor(List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis,
								 ImpactedTestsProvider impactedTestsProvider) {
		super(testwiseCoverageAgentApis);
		this.impactedTestsProvider = impactedTestsProvider;
	}

	@Override
	public List<TestExecution> execute(TestExecutorRequest executorRequest) {
		AvailableTests availableTestDetails = TestDescriptorUtils
				.getAvailableTests(executorRequest.testEngine, executorRequest.engineTestDescriptor);
		List<PrioritizableTestCluster> testClusters = impactedTestsProvider.getImpactedTestsFromTeamscale(
				availableTestDetails.getTestList());

		if (testClusters == null) {
			LOGGER.debug(() -> "Falling back to execute all!");
			return super.execute(executorRequest);
		}

		AutoSkippingEngineExecutionListener executionListener = new AutoSkippingEngineExecutionListener(
				getImpactedTestUniqueIds(availableTestDetails, testClusters),
				executorRequest.engineExecutionListener, executorRequest.engineTestDescriptor);

		List<TestExecution> testExecutions = new ArrayList<>();

		LOGGER.debug(() -> "Re-discovering tests for delegate engine " + executorRequest.testEngine.getId());

		for (PrioritizableTestCluster testCluster : testClusters) {
			Set<UniqueId> uniqueIdsOfTestsToExecute = availableTestDetails.convertToUniqueIds(testCluster.tests);
			UniqueIdsDiscoveryRequest engineDiscoveryRequest = new UniqueIdsDiscoveryRequest(
					uniqueIdsOfTestsToExecute, executorRequest.configurationParameters);
			TestDescriptor testDescriptor = executorRequest.testEngine.discover(engineDiscoveryRequest,
					UniqueId.forEngine(executorRequest.testEngine.getId()));
			TestExecutorRequest impactedExecutorRequest = new TestExecutorRequest(executorRequest.testEngine,
					testDescriptor, executionListener, executorRequest.configurationParameters);
			List<TestExecution> testExecutionsForCluster = super
					.execute(impactedExecutorRequest);

			testExecutions.addAll(testExecutionsForCluster);
		}

		return testExecutions;
	}

	private static Set<UniqueId> getImpactedTestUniqueIds(AvailableTests availableTests,
														  List<PrioritizableTestCluster> testClusters) {
		return testClusters.stream()
				.flatMap(testCluster -> availableTests.convertToUniqueIds(testCluster.tests).stream())
				.collect(Collectors.toSet());
	}
}
