package com.teamscale.tia.client;

import java.time.Instant;
import java.util.List;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTestCluster;

import okhttp3.HttpUrl;

/**
 * Communicates with one Teamscale JaCoCo agent in testwise coverage mode to facilitate the Test Impact analysis.
 * <p>
 * Use this class to retrieve impacted tests from Teamscale, start a {@link TestRun} based on these selected and
 * prioritized tests and upload test-wise coverage after having executed the tests.
 * <p>
 * The caller of this class is responsible for actually executing the tests.
 *
 * <h1>Available Tests</h1>
 * This API allows you to pass a list of available tests to start a test run. This list is used for multiple purposes:
 *
 * <ul>
 *     <li>To determine if any tests changed, were added or removed: Teamscale will not suggest deleted tests and
 *     will always suggest changed or added tests.
 *     <li>To cluster tests: The list of available tests includes information about test clusters, which are logical
 *     groups of tests. Teamscale will only prioritize tests against each other if they are in the same cluster.
 * </ul>
 */
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
	 * Starts a test run but does not ask Teamscale to prioritize and select any test cases. Use this when you only want
	 * to record test-wise coverage and don't care about TIA's test selection and prioritization.
	 */
	public TestRun startTestRunWithoutTestSelection() {
		return new TestRun(api);
	}

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * considers all changes since the last time that test-wise coverage was uploaded. In most situations this is the
	 * optimal behaviour.
	 *
	 * @param availableTests A list of all available tests. This is used to determine which tests need to be run, e.g.
	 *                       because they are completely new or changed since the last run. If you provide an empty
	 *                       list, no tests will be selected. The clustering information in this list is used to
	 *                       construct the test clusters in the returned {@link TestRunWithClusteredSuggestions}.
	 * @throws AgentHttpRequestFailedException e.g. if the agent or Teamscale is not reachable or an internal error
	 *                                         occurs. This method already retries the request once, so this is likely a
	 *                                         terminal failure. You should simply fall back to running all tests in
	 *                                         this case and not communicate further with the agent. You should visibly
	 *                                         report this problem so it can be fixed.
	 */
	public TestRunWithClusteredSuggestions startTestRun(
			List<ClusteredTestDetails> availableTests) throws AgentHttpRequestFailedException {
		return startTestRun(availableTests, null, null);
	}

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * considers all changes since the last time that test-wise coverage was uploaded. In most situations this is the
	 * optimal behaviour.
	 * <p>
	 * Using this method, Teamscale will perform the selection and prioritization based on the tests it currently knows
	 * about. In this case, it will not automatically include changed or new tests in the selection (since it doesn't
	 * know about these changes) and it may return deleted tests (since it doesn't know about the deletions). It will
	 * also not cluster the tests as {@link #startTestRun(List)} would.
	 * <p>
	 * <strong>Thus, we recommend that, if possible, you use {@link #startTestRun(List)} instead.</strong>
	 *
	 * @throws AgentHttpRequestFailedException e.g. if the agent or Teamscale is not reachable or an internal error
	 *                                         occurs. This method already retries the request once, so this is likely a
	 *                                         terminal failure. You should simply fall back to running all tests in
	 *                                         this case and not communicate further with the agent. You should visibly
	 *                                         report this problem so it can be fixed.
	 */
	public TestRunWithFlatSuggestions startTestRunAssumingUnchangedTests() throws AgentHttpRequestFailedException {
		return startTestRunAssumingUnchangedTests(null, null);
	}

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * considers all changes since the given baseline timestamp.
	 *
	 * @param availableTests A list of all available tests. This is used to determine which tests need to be run, e.g.
	 *                       because they are completely new or changed since the last run. If you provide an empty
	 *                       list, no tests will be selected.The clustering information in this list is used to
	 *                       construct the test clusters in the returned {@link TestRunWithClusteredSuggestions}.
	 * @param baseline       Consider all code changes since this date when calculating the impacted tests.
	 * @param baselineRevision Same as baseline but accepts a revision (e.g. git SHA1) instead of a branch and timestamp
	 * @throws AgentHttpRequestFailedException e.g. if the agent or Teamscale is not reachable or an internal error
	 *                                         occurs. You should simply fall back to running all tests in this case.
	 */
	public TestRunWithClusteredSuggestions startTestRun(List<ClusteredTestDetails> availableTests,
														Instant baseline, String baselineRevision) throws AgentHttpRequestFailedException {
		if (availableTests == null) {
			throw new IllegalArgumentException("availableTests must not be null. If you cannot provide a list of" +
					" available tests, please use startTestRunAssumingUnchangedTests instead - but please be aware" +
					" that this method of using the TIA cannot take into account changes in the tests themselves.");
		}
		Long baselineTimestamp = calculateBaselineTimestamp(baseline);
		List<PrioritizableTestCluster> clusters = AgentCommunicationUtils.handleRequestError(
				() -> api.testRunStarted(includeNonImpactedTests, baselineTimestamp, baselineRevision, availableTests),
				"Failed to start the test run");
		return new TestRunWithClusteredSuggestions(api, clusters);
	}

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * considers all changes since the given baseline timestamp.
	 * <p>
	 * Using this method, Teamscale will perform the selection and prioritization based on the tests it currently knows
	 * about. In this case, it will not automatically include changed or new tests in the selection (since it doesn't
	 * know about these changes) and it may return deleted tests (since it doesn't know about the deletions). It will *
	 * also not cluster the tests as {@link #startTestRun(List, Instant, String)} would.
	 * <p>
	 * <strong>Thus, we recommend that, if possible, you use {@link #startTestRun(List, Instant, String)}
	 * instead.</strong>
	 *
	 * @throws AgentHttpRequestFailedException e.g. if the agent or Teamscale is not reachable or an internal error
	 *                                         occurs. This method already retries the request once, so this is likely a
	 *                                         terminal failure. You should simply fall back to running all tests in
	 *                                         this case and not communicate further with the agent. You should visibly
	 *                                         report this problem so it can be fixed.
	 */
	public TestRunWithFlatSuggestions startTestRunAssumingUnchangedTests(
			Instant baseline, String baselineRevision) throws AgentHttpRequestFailedException {
		Long baselineTimestamp = calculateBaselineTimestamp(baseline);
		List<PrioritizableTestCluster> clusters = AgentCommunicationUtils.handleRequestError(
				() -> api.testRunStarted(includeNonImpactedTests, baselineTimestamp, baselineRevision),
				"Failed to start the test run");
		return new TestRunWithFlatSuggestions(api, clusters.get(0).tests);
	}

	private Long calculateBaselineTimestamp(Instant baseline) {
		Long baselineTimestamp = null;
		if (baseline != null) {
			baselineTimestamp = baseline.toEpochMilli();
		}
		return baselineTimestamp;
	}


}
