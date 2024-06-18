package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.StringUtils;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.test_impacted.commons.LoggerUtils;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for retrieving the impacted {@link PrioritizableTestCluster}s corresponding to {@link ClusteredTestDetails}
 * available for test execution.
 */
public class ImpactedTestsProvider {

	private static final Logger LOGGER = LoggerUtils.getLogger(ImpactedTestsProvider.class);

	private final TeamscaleClient client;

	private final String baseline;

	private final String baselineRevision;

	private final CommitDescriptor endCommit;

	private final String endRevision;

	private final String repository;

	/**
	 * The partition for the tests provided.
	 */
	public final String partition;

	private final boolean includeNonImpacted;

	private final boolean includeAddedTests;

	private final boolean includeFailedAndSkipped;

	public ImpactedTestsProvider(TeamscaleClient client, String baseline, String baselineRevision, CommitDescriptor endCommit, String endRevision, String repository, String partition,
								 boolean includeNonImpacted, boolean includeAddedTests,
								 boolean includeFailedAndSkipped) {
		this.client = client;
		this.baseline = baseline;
		this.baselineRevision = baselineRevision;
		this.endCommit = endCommit;
		this.endRevision = endRevision;
		this.repository = repository;
		this.partition = partition;
		this.includeNonImpacted = includeNonImpacted;
		this.includeAddedTests = includeAddedTests;
		this.includeFailedAndSkipped = includeFailedAndSkipped;
	}

	/** Queries Teamscale for impacted tests. */
	public List<PrioritizableTestCluster> getImpactedTestsFromTeamscale(
			List<ClusteredTestDetails> availableTestDetails) {
		try {
			LOGGER.info(() -> "Getting impacted tests...");
			Response<List<PrioritizableTestCluster>> response;
			if (!StringUtils.isBlank(endRevision)) {
				response = client
						.getImpactedTests(availableTestDetails, baseline, baselineRevision, endRevision, repository, Collections.singletonList(partition),
								includeNonImpacted, includeAddedTests, includeFailedAndSkipped);
			} else if (endCommit != null){
				response = client
						.getImpactedTests(availableTestDetails, baseline, baselineRevision, endCommit, repository,
								Collections.singletonList(partition),
								includeNonImpacted, includeAddedTests, includeFailedAndSkipped);
			} else {
				LOGGER.severe(() -> "Retrieving impacted tests failed. Please either provide an endRevision or an endCommit.");
				return null;
			}
			if (response.isSuccessful()) {
				List<PrioritizableTestCluster> testClusters = response.body();
				if (testClusters != null && testCountIsPlausible(testClusters, availableTestDetails)) {
					return testClusters;

				}
				LOGGER.severe("Teamscale was not able to determine impacted tests:\n" + response.body());
			} else {
				LOGGER.severe("Retrieval of impacted tests failed: " + response.code() + " " + response
						.message() + "\n" + getErrorBody(response));
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e, () -> "Retrieval of impacted tests failed.");
		}
		return null;
	}

	private static String getErrorBody(Response<?> response) throws IOException {
		try (ResponseBody error = response.errorBody()) {
			if (error != null) {
				return error.string();
			}
		}
		return "";
	}

	/**
	 * Checks that the number of tests returned by Teamscale matches the number of available tests when running with
	 * {@link #includeNonImpacted}.
	 */
	private boolean testCountIsPlausible(List<PrioritizableTestCluster> testClusters,
										 List<ClusteredTestDetails> availableTestDetails) {
		long returnedTests = testClusters.stream().mapToLong(g -> g.tests.size()).sum();
		if (!this.includeNonImpacted) {
			LOGGER.info(
					() -> "Received " + returnedTests + " impacted tests of " + availableTestDetails.size() + " available tests.");
			return true;
		}
		if (returnedTests == availableTestDetails.size()) {
			return true;
		} else {
			LOGGER.severe(
					() -> "Retrieved " + returnedTests + " tests from Teamscale, but expected " + availableTestDetails
							.size() + ".");
			return false;
		}
	}
}
