package com.teamscale.jacoco.agent.testimpact;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTestCluster;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.jacoco.agent.JacocoRuntimeController;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.testwise.model.TestExecution;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

/** Base class for strategies to handle test start and end events. */
public abstract class TestEventHandlerStrategyBase {

	private final Logger logger = LoggingUtils.getLogger(this);

	/** Controls the JaCoCo runtime. */
	protected final JacocoRuntimeController controller;

	/** The timestamp at which the /test/start endpoint has been called last time. */
	private long startTimestamp;

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
		// Dump and reset coverage so that we only record coverage that belongs to this particular test case.
		controller.reset();
		controller.setSessionId(test);
		startTimestamp = System.currentTimeMillis();
	}

	/**
	 * Called when the test with the given name finished.
	 *
	 * @param test          Uniform path of the test
	 * @param testExecution A test execution object holding the test result and error message. May be null if non is
	 *                      given in the request.
	 * @return The body of the response. <code>null</code> indicates "204 No content". Non-null results will be treated
	 * as json response.
	 */
	public String testEnd(String test, TestExecution testExecution) throws JacocoRuntimeController.DumpException {
		if (testExecution != null) {
			long endTimestamp = System.currentTimeMillis();
			testExecution.setDurationMillis(endTimestamp - startTimestamp);
		}
		return null;
	}

	public String testRunStart(List<ClusteredTestDetails> availableTests, boolean includeNonImpactedTests,
							   Long baseline) throws IOException {
		if (teamscaleClient == null) {
			throw new UnsupportedOperationException("You did not configure a connection to Teamscale in the agent." +
					" Thus, you cannot use the agent to retrieve impacted tests via the testrun/start REST endpoint." +
					" Please use the 'teamscale-' agent parameters to configure a Teamscale connection.");
		}

		Response<List<PrioritizableTestCluster>> response = teamscaleClient
				.getImpactedTests(availableTests, baseline, agentOptions.getTeamscaleServerOptions().commit,
						agentOptions.getTeamscaleServerOptions().partition, includeNonImpactedTests);
		if (response.isSuccessful()) {
			String json = prioritizableTestClustersJsonAdapter.toJson(response.body());
			logger.debug("Teamscale suggested these tests: {}", json);
			return json;
		} else {
			ResponseBody errorBody = response.errorBody();
			String responseBody = "<no response body provided>";
			if (errorBody != null) {
				responseBody = errorBody.string();
			}
			throw new IOException(
					"Request to Teamscale to get impacted tests failed with HTTP status " + response.code() +
							" " + response.message() + ". Response body: " + responseBody);
		}
	}

	public void testRunEnd() throws IOException {
		// base implementation does nothing
	}
}
