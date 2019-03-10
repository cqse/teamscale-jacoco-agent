package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TestClusterForPrioritization;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.test_impacted.engine.options.ServerOptions;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Test executor that only executes impacted tests and collects test wise coverage for the executed tests. */
public class ImpactedTestsExecutor extends TestwiseCoverageCollectingTestExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactedTestsExecutor.class);

	private final ServerOptions serverOptions;

	private final Long baseline;

	private final CommitDescriptor endCommit;

	private final String partition;

	public ImpactedTestsExecutor(List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis, ServerOptions serverOptions, Long baseline, CommitDescriptor endCommit, String partition) {
		super(testwiseCoverageAgentApis);
		this.serverOptions = serverOptions;
		this.baseline = baseline;
		this.endCommit = endCommit;
		this.partition = partition;
	}

	@Override
	public List<TestExecution> execute(TestExecutorRequest executorRequest) {
		AvailableTests availableTestDetails = TestDescriptorUtils
				.getAvailableTests(executorRequest.testEngine, executorRequest.engineTestDescriptor);
		List<TestClusterForPrioritization> testClustersForPrioritization = getImpactedTestsFromTeamscale(
				availableTestDetails.getTestList());
		AutoSkippingEngineExecutionListener executionListener = new AutoSkippingEngineExecutionListener(
				getImpactedTestUniqueIds(availableTestDetails, testClustersForPrioritization),
				executorRequest.engineExecutionListener, executorRequest.engineTestDescriptor);

		List<TestExecution> testExecutions = new ArrayList<>();

		LOGGER.debug(() -> "Re-discovering tests for delegate engine " + executorRequest.testEngine.getId());

		for (TestClusterForPrioritization testClusterForPrioritization : testClustersForPrioritization) {
			Set<UniqueId> uniqueIdsOfTestsToExecute = availableTestDetails
					.convertToUniqueIds(testClusterForPrioritization.testsForPrioritization);
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

	private static Set<UniqueId> getImpactedTestUniqueIds(AvailableTests availableTests, List<TestClusterForPrioritization> testClustersForPrioritzation) {
		Set<UniqueId> result = new HashSet<>();
		for (TestClusterForPrioritization testClusterForPrioritization : testClustersForPrioritzation) {
			result.addAll(availableTests.convertToUniqueIds(testClusterForPrioritization.testsForPrioritization));
		}
		return result;
	}

	/** Queries Teamscale for impacted tests. */
	private List<TestClusterForPrioritization> getImpactedTestsFromTeamscale(List<ClusteredTestDetails> availableTestDetails) {
		try {
			LOGGER.info(() -> "Getting impacted tests...");
			TeamscaleClient client = new TeamscaleClient(serverOptions.getUrl(), serverOptions.getUserName(),
					serverOptions.getUserAccessToken(), serverOptions.getProject());
			Response<List<TestClusterForPrioritization>> response = client
					.getImpactedTests(availableTestDetails, baseline, endCommit, partition);
			if (response.isSuccessful()) {
				List<TestClusterForPrioritization> testList = response.body();
				if (testList == null) {
					LOGGER.error(() -> "Teamscale was not able to determine impacted tests:\n" + response.errorBody());
				}
				return testList;
			} else {
				LOGGER.error(() -> "Retrieval of impacted tests failed: " + response.code() + " " + response
						.message() + "\n" + response.errorBody());
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Retrieval of impacted tests failed.");
		}
		return Collections.emptyList();
	}

}
