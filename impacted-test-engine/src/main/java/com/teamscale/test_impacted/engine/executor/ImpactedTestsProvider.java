package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


/**
 * Class for retrieving the impacted {@link PrioritizableTestCluster}s corresponding to {@link ClusteredTestDetails}
 * available for test execution.
 */
public class ImpactedTestsProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactedTestsProvider.class);

	private final TeamscaleClient client;

	private final String baseline;

	private final CommitDescriptor endCommit;

	private final String partition;

	private final boolean includeNonImpacted;

	private final boolean includeAddedTests;

	public ImpactedTestsProvider(TeamscaleClient client, String baseline, CommitDescriptor endCommit, String partition,
								 boolean includeNonImpacted, boolean includeAddedTests) {
		this.client = client;
		this.baseline = baseline;
		this.endCommit = endCommit;
		this.partition = partition;
		this.includeNonImpacted = includeNonImpacted;
		this.includeAddedTests = includeAddedTests;
	}

	/** Queries Teamscale for impacted tests. */
	public List<PrioritizableTestCluster> getImpactedTestsFromTeamscale(
			List<ClusteredTestDetails> availableTestDetails) {
		try {
			LOGGER.info(() -> "Getting impacted tests...");
			Response<List<PrioritizableTestCluster>> response = client
					.getImpactedTests(availableTestDetails, baseline, endCommit, Collections.singletonList(partition),
							includeNonImpacted, includeAddedTests);
			if (response.isSuccessful()) {
				List<PrioritizableTestCluster> testClusters = response.body();
				if (testClusters != null && testCountIsPlausible(testClusters, availableTestDetails)) {
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

	/**
	 * Checks that the number of tests returned by Teamscale matches the number of available tests when running with
	 * {@link #includeNonImpacted}.
	 */
	private boolean testCountIsPlausible(List<PrioritizableTestCluster> testClusters,
										 List<ClusteredTestDetails> availableTestDetails) {
		if (!this.includeNonImpacted) {
			return true;
		}
		long returnedTests = testClusters.stream().mapToLong(g -> g.tests.size()).sum();
		if (returnedTests == availableTestDetails.size()) {
			return true;
		} else {
			LOGGER.error(
					() -> "Retrieved " + returnedTests + " tests from Teamscale, but expected " + availableTestDetails
							.size() + ".");
			return false;
		}
	}
}
