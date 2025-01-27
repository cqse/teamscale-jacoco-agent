package com.teamscale.tia.client

import com.teamscale.client.ClusteredTestDetails
import com.teamscale.tia.client.AgentCommunicationUtils.handleRequestError
import okhttp3.HttpUrl
import java.time.Instant

/**
 * Communicates with one Teamscale JaCoCo agent in testwise coverage mode to facilitate the Test Impact analysis.
 *
 *
 * Use this class to retrieve impacted tests from Teamscale, start a [TestRun] based on these selected and
 * prioritized tests and upload test-wise coverage after having executed the tests.
 *
 *
 * The caller of this class is responsible for actually executing the tests.
 *
 * <h1>Available Tests</h1>
 * This API allows you to pass a list of available tests to start a test run. This list is used for multiple purposes:
 *
 *
 *  * To determine if any tests changed, were added or removed: Teamscale will not suggest deleted tests and
 * will always suggest changed or added tests.
 *  * To cluster tests: The list of available tests includes information about test clusters, which are logical
 * groups of tests. Teamscale will only prioritize tests against each other if they are in the same cluster.
 *
 */
class TiaAgent(private val includeNonImpactedTests: Boolean, url: HttpUrl) {
	private val api = ITestwiseCoverageAgentApi.createService(url)

	/**
	 * Starts a test run but does not ask Teamscale to prioritize and select any test cases. Use this when you only want
	 * to record test-wise coverage and don't care about TIA's test selection and prioritization.
	 */
	fun startTestRunWithoutTestSelection() = TestRun(api)

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * considers all changes since the last time that test-wise coverage was uploaded. In most situations this is the
	 * optimal behaviour.
	 *
	 * @param availableTests A list of all available tests. This is used to determine which tests need to be run, e.g.
	 * because they are completely new or changed since the last run. If you provide an empty
	 * list, no tests will be selected. The clustering information in this list is used to
	 * construct the test clusters in the returned [TestRunWithClusteredSuggestions].
	 * @throws AgentHttpRequestFailedException e.g. if the agent or Teamscale is not reachable or an internal error
	 * occurs. This method already retries the request once, so this is likely a
	 * terminal failure. You should simply fall back to running all tests in
	 * this case and not communicate further with the agent. You should visibly
	 * report this problem so it can be fixed.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun startTestRun(
		availableTests: List<ClusteredTestDetails>
	) = startTestRun(availableTests, null, null)

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * has considered all changes since the last time that test-wise coverage was uploaded.
	 * In most situations, this is the optimal behavior.
	 *
	 *
	 * Using this method, Teamscale will perform the selection and prioritization based on the tests it currently knows
	 * about. In this case, it will not automatically include changed or new tests in the selection (since it doesn't
	 * know about these changes) and it may return deleted tests (since it doesn't know about the deletions). It will
	 * also not cluster the tests as [.startTestRun] would.
	 *
	 *
	 * **Thus, we recommend that, if possible, you use [.startTestRun] instead.**
	 *
	 * @throws AgentHttpRequestFailedException e.g., if the agent or Teamscale is not reachable or an internal error
	 * occurs. This method already retries the request once, so this is likely a
	 * terminal failure. You should fall back to running all tests in
	 * this case and not communicate further with the agent. You should visibly
	 * report this problem so it can be fixed.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun startTestRunAssumingUnchangedTests() =
		startTestRunAssumingUnchangedTests(null, null)

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * has considered all changes since the given baseline timestamp.
	 *
	 * @param availableTests A list of all available tests. This is used to determine which tests need to be run, e.g.,
	 * because they are completely new or changed since the last run. If you provide an empty
	 * list, no tests will be selected. The clustering information in this list is used to
	 * construct the test clusters in the returned [TestRunWithClusteredSuggestions].
	 * @param baseline       Consider all code changes since this date when calculating the impacted tests.
	 * @param baselineRevision Same as baseline but accepts a revision (e.g., git SHA1) instead of a branch and timestamp
	 * @throws AgentHttpRequestFailedException e.g., if the agent or Teamscale is not reachable or an internal error
	 * occurs.
	 * You should fall back to running all tests in this case.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun startTestRun(
		availableTests: List<ClusteredTestDetails>,
		baseline: Instant?,
		baselineRevision: String?
	): TestRunWithClusteredSuggestions {
		val baselineTimestamp = baseline?.toEpochMilli()
		val clusters = handleRequestError("Failed to start the test run") {
			api.testRunStarted(
				includeNonImpactedTests,
				baselineTimestamp,
				baselineRevision,
				availableTests
			)
		}
		return TestRunWithClusteredSuggestions(api, clusters)
	}

	/**
	 * Runs the TIA to determine which of the given available tests should be run and in which order. This method
	 * has considered all changes since the given baseline timestamp.
	 *
	 *
	 * Using this method, Teamscale will perform the selection and prioritization based on the tests it currently knows
	 * about. In this case, it will not automatically include changed or new tests in the selection (since it doesn't
	 * know about these changes) and it may return deleted tests (since it doesn't know about the deletions). It will *
	 * also not cluster the tests as [startTestRun] would.
	 *
	 *
	 * **Thus, we recommend that, if possible, you use [startTestRun]
	 * instead.**
	 *
	 * @throws AgentHttpRequestFailedException e.g., if the agent or Teamscale is not reachable or an internal error
	 * occurs. This method already retries the request once, so this is likely a
	 * terminal failure. You should fall back to running all tests in
	 * this case and not communicate further with the agent. You should visibly
	 * report this problem so it can be fixed.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun startTestRunAssumingUnchangedTests(
		baseline: Instant?, baselineRevision: String?
	): TestRunWithFlatSuggestions {
		val clusters = handleRequestError("Failed to start the test run") {
			api.testRunStarted(includeNonImpactedTests, baseline?.toEpochMilli(), baselineRevision)
		}
		return TestRunWithFlatSuggestions(api, clusters?.firstOrNull()?.tests ?: emptyList())
	}
}
