package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.test_impacted.engine.options.ServerOptions;
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Test executor that only executes impacted tests and collects test wise coverage for the executed tests. */
public class ImpactedTestsExecutor extends TestwiseCoverageCollectingTestExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactedTestsExecutor.class);

	private final ServerOptions serverOptions;

	private final Long baseline;

	private final CommitDescriptor endCommit;

	private final String partition;

	private File requestLogFile;

	public ImpactedTestsExecutor(List<ITestwiseCoverageAgentApi> testwiseCoverageAgentApis, ServerOptions serverOptions,
								 Long baseline, CommitDescriptor endCommit, String partition,
								 File requestLogFile) {
		super(testwiseCoverageAgentApis);
		this.serverOptions = serverOptions;
		this.baseline = baseline;
		this.endCommit = endCommit;
		this.partition = partition;
		this.requestLogFile = requestLogFile;
	}

	@Override
	public List<TestExecution> execute(TestExecutorRequest executorRequest) {
		AvailableTests availableTestDetails = TestDescriptorUtils
				.getAvailableTests(executorRequest.testEngine, executorRequest.engineTestDescriptor);
		List<PrioritizableTestCluster> testClusters = getImpactedTestsFromTeamscale(
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

	/** Queries Teamscale for impacted tests. */
	private List<PrioritizableTestCluster> getImpactedTestsFromTeamscale(
			List<ClusteredTestDetails> availableTestDetails) {
		try {
			LOGGER.info(() -> "Getting impacted tests...");
			TeamscaleClient client = new TeamscaleClient(serverOptions.getUrl(), serverOptions.getUserName(),
					serverOptions.getUserAccessToken(), serverOptions.getProject(), requestLogFile);
			Response<List<PrioritizableTestCluster>> response = client
					.getImpactedTests(availableTestDetails, baseline, endCommit, partition);
			if (response.isSuccessful()) {
				List<PrioritizableTestCluster> testClusters = response.body();
				if (testClusters != null) {
					return testClusters;
				}
				LOGGER.error(() -> "Teamscale was not able to determine impacted tests:\n" + response.errorBody());
			} else {
				LOGGER.error(() -> "Retrieval of impacted tests failed: " + response.code() + " " + response
						.message() + "\n" + response.errorBody());
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Retrieval of impacted tests failed.");
		}
		return null;
	}

}
