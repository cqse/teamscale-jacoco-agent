package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

public class ImpactedTestsProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactedTestsProvider.class);

	private final TeamscaleClient client;

	private final Long baseline;

	private final CommitDescriptor endCommit;

	private final String partition;

	public ImpactedTestsProvider(TeamscaleClient client, Long baseline, CommitDescriptor endCommit, String partition) {
		this.client = client;
		this.baseline = baseline;
		this.endCommit = endCommit;
		this.partition = partition;
	}

	/** Queries Teamscale for impacted tests. */
	public List<PrioritizableTestCluster> getImpactedTestsFromTeamscale(
			List<ClusteredTestDetails> availableTestDetails) {
		try {
			LOGGER.info(() -> "Getting impacted tests...");
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
