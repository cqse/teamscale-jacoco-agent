package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.HttpUtils;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import com.teamscale.report.testwise.model.TestExecution;
import org.slf4j.Logger;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** Base class for strategies to handle test events. */
public abstract class TestEventHandlerStrategyBase {

	private final Logger logger = LoggingUtils.getLogger(this);

	/** Controls the JaCoCo runtime. */
	protected final JacocoRuntimeController controller;

	/** The timestamp at which the /test/start endpoint has been called last time. */
	private long startTimestamp = -1;

	/** The options the user has configured for the agent. */
	protected final AgentOptions agentOptions;

	/** May be null if the user did not configure Teamscale. */
	protected final TeamscaleClient teamscaleClient;

	private final JsonAdapter<List<PrioritizableTestCluster>> prioritizableTestClustersJsonAdapter = new Moshi.Builder()
			.build().adapter(Types.newParameterizedType(List.class, PrioritizableTestCluster.class));


	protected TestEventHandlerStrategyBase(AgentOptions agentOptions, JacocoRuntimeController controller) {
		this.controller = controller;
		this.agentOptions = agentOptions;
		this.teamscaleClient = agentOptions.createTeamscaleClient();
	}

	/** Called when test test with the given name is about to start. */
	public void testStart(String test) {
		logger.debug("Test {} started", test);
		// Reset coverage so that we only record coverage that belongs to this particular test case.
		controller.reset();
		controller.setSessionId(test);
		startTimestamp = System.currentTimeMillis();
	}

	/**
	 * Called when the test with the given name finished.
	 *
	 * @param test          Uniform path of the test
	 * @param testExecution A test execution object holding the test result and error message. May be null if none is
	 *                      given in the request.
	 * @return The body of the response. <code>null</code> indicates "204 No content". Non-null results will be treated
	 * as a json response.
	 */
	public String testEnd(String test,
						  TestExecution testExecution) throws JacocoRuntimeController.DumpException, CoverageGenerationException {
		if (testExecution != null) {
			testExecution.setUniformPath(test);
			if (startTimestamp != -1) {
				long endTimestamp = System.currentTimeMillis();
				testExecution.setDurationMillis(endTimestamp - startTimestamp);
			}
		}
		logger.debug("Test {} ended with test execution {}", test, testExecution);
		return null;
	}

	/**
	 * Retrieves impacted tests from Teamscale, if a {@link #teamscaleClient} has been configured.
	 *
	 * @param availableTests          List of all available tests that could be run or null if the user does not want to
	 *                                provide one.
	 * @param includeNonImpactedTests If this is true, only performs prioritization, no selection.
	 * @param baseline                Optional baseline for the considered changes.
	 * @throws IOException                   if the request to Teamscale failed.
	 * @throws UnsupportedOperationException if the user did not properly configure the {@link #teamscaleClient}.
	 */
	public String testRunStart(List<ClusteredTestDetails> availableTests, boolean includeNonImpactedTests,
							   String baseline) throws IOException {
		int availableTestCount = 0;
		if (availableTests != null) {
			availableTestCount = availableTests.size();
		}
		logger.debug("Test run started with {} available tests. baseline = {}, includeNonImpactedTests = {}",
				availableTestCount, baseline, includeNonImpactedTests);
		if (teamscaleClient == null) {
			throw new UnsupportedOperationException("You did not configure a connection to Teamscale in the agent." +
					" Thus, you cannot use the agent to retrieve impacted tests via the testrun/start REST endpoint." +
					" Please use the 'teamscale-' agent parameters to configure a Teamscale connection.");
		}
		if (agentOptions.getTeamscaleServerOptions().commit == null) {
			throw new UnsupportedOperationException(
					"You did not provide a '" + AgentOptions.TEAMSCALE_COMMIT_OPTION + "' or '" +
							AgentOptions.TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION + "'. '" +
							AgentOptions.TEAMSCALE_REVISION_OPTION + "' is not sufficient to retrieve impacted tests.");
		}

		Response<List<PrioritizableTestCluster>> response = teamscaleClient
				.getImpactedTests(availableTests, baseline, agentOptions.getTeamscaleServerOptions().commit,
						Collections.singletonList(agentOptions.getTeamscaleServerOptions().partition), includeNonImpactedTests);
		if (response.isSuccessful()) {
			String json = prioritizableTestClustersJsonAdapter.toJson(response.body());
			logger.debug("Teamscale suggested these tests: {}", json);
			return json;
		} else {
			String responseBody = HttpUtils.getErrorBodyStringSafe(response);
			throw new IOException(
					"Request to Teamscale to get impacted tests failed with HTTP status " + response.code() +
							" " + response.message() + ". Response body: " + responseBody);
		}
	}

	/**
	 * Signals that the test run has ended. Strategies that support this can upload a report via the {@link
	 * #teamscaleClient} here.
	 */
	public void testRunEnd() throws IOException, CoverageGenerationException {
		throw new UnsupportedOperationException("You configured the agent in a mode that does not support uploading " +
				"reports to Teamscale. Please configure 'tia-mode=teamscale-upload' or simply don't call" +
				"POST /testrun/end.");
	}
}
