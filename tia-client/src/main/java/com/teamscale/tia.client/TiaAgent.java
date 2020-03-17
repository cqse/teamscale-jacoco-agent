package com.teamscale.tia.client;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTestCluster;
import okhttp3.HttpUrl;

import java.time.Instant;
import java.util.List;

/**
 * Communicates with one Teamscale JaCoCo agent in test-wise coverage mode to facilitate the Test Impact analysis.
 * <p>
 * Use this class to retrieve impacted tests from Teamscale, start a {@link TestRun} based on these selected and
 * prioritized tests and upload test-wise coverage after having executed the tests.
 * <p>
 * The caller of this class is responsible for actually executing the tests.
 */
@SuppressWarnings("unused")
public class TiaAgent {

	private final boolean includeNonImpactedTests;
	private final ITestwiseCoverageAgentApi api;

	/**
	 * @param includeNonImpactedTests if this is true, only prioritization is performed, no test selection.
	 * @param url                     URL under which the agent is reachable.
	 */
	public TiaAgent(boolean includeNonImpactedTests, HttpUrl url) {
		this.includeNonImpactedTests = includeNonImpactedTests;
		api = ITestwiseCoverageAgentApi.createService(url);
	}

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * considers all changes since the last time that test-wise coverage was uploaded. In most situations this is the
	 * intended behaviour.
	 *
	 * @throws AgentHttpRequestFailedException e.g. if the agent or Teamscale is not reachable or an internal error
	 *                                         occurs. This method already retries the request once, so this is likely a
	 *                                         terminal failure. You should simply fall back to running all tests in
	 *                                         this case and not communicate further with the agent. You should visibly
	 *                                         report this problem so it can be fixed.
	 */
	public TestRun startTestRun(List<ClusteredTestDetails> availableTests) throws AgentHttpRequestFailedException {
		return startTestRun(availableTests, null);
	}

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order.
	 *
	 * @param baseline Considers all changes since this date.
	 * @throws AgentHttpRequestFailedException e.g. if the agent or Teamscale is not reachable or an internal error
	 *                                         occurs. You should simply fall back to running all tests in this case.
	 */
	public TestRun startTestRun(List<ClusteredTestDetails> availableTests,
								Instant baseline) throws AgentHttpRequestFailedException {
		Long baselineTimestamp = null;
		if (baseline != null) {
			baselineTimestamp = baseline.toEpochMilli();
		}
		List<PrioritizableTestCluster> clusters = AgentCommunicationUtils.handleRequestError(
				api.testRunStarted(includeNonImpactedTests, baselineTimestamp, availableTests),
				"Failed to start the test run");
		return new TestRun(api, clusters);
	}


}
