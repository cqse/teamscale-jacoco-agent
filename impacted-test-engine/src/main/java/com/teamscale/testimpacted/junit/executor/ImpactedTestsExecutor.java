package com.teamscale.testimpacted.junit.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TestClusterForPrioritization;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.testimpacted.controllers.ITestwiseCoverageAgentApi;
import com.teamscale.testimpacted.junit.options.ServerOptions;
import com.teamscale.testimpacted.test_descriptor.TestDescriptorUtils;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImpactedTestsExecutor extends TestWiseCoverageCollectingTestExecutor {

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

	public List<TestExecution> execute(TestExecutorRequest testExecutorRequest) {
		AvailableTests availableTestDetails = TestDescriptorUtils
				.getAvailableTests(testExecutorRequest.testEngine, testExecutorRequest.engineTestDescriptor);
		List<TestClusterForPrioritization> testClustersForPrioritization = getImpactedTestsFromTeamscale(
				availableTestDetails.getTestList());
		List<TestExecution> testExecutions = new ArrayList<>();

		LOGGER.debug(() -> "Re-discovering tests for delegate engine " + testExecutorRequest.testEngine.getId());

		for (TestClusterForPrioritization testClusterForPrioritization : testClustersForPrioritization) {
			UniqueIdsDiscoveryRequest engineDiscoveryRequest = new UniqueIdsDiscoveryRequest(
					availableTestDetails.convertToUniqueIds(testClusterForPrioritization.testsForPrioritization),
					testExecutorRequest.configurationParameters);
			TestDescriptor testDescriptor = testExecutorRequest.testEngine.discover(engineDiscoveryRequest,
					UniqueId.forEngine(testExecutorRequest.testEngine.getId()));
			List<TestExecution> testExecutionsForCluster = super.execute(
					new TestExecutorRequest(testExecutorRequest.testEngine, testDescriptor,
							testExecutorRequest.engineExecutionListener, testExecutorRequest.configurationParameters));

			testExecutions.addAll(testExecutionsForCluster);
		}

		return testExecutions;
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
					LOGGER.error(() -> "Teamscale was not able to determine impacted tests.");
				}
				return testList;
			} else {
				LOGGER.error(() -> "Retrieval of impacted tests failed: " + response.code() + " " + response.message());
			}
		} catch (IOException e) {
			LOGGER.error(e, () -> "Retrieval of impacted tests failed.");
		}
		return Collections.emptyList();
	}

}
