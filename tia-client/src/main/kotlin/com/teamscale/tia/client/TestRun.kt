package com.teamscale.tia.client

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.tia.client.AgentCommunicationUtils.handleRequestError
import com.teamscale.tia.client.UrlUtils.encodeUrl

/**
 * Use this class to report test start and end events and upload testwise coverage to Teamscale.
 *
 *
 * After having run all tests, call [.endTestRun] to create a testwise coverage report and upload it to
 * Teamscale. This requires that you configured the agent to upload coverage to Teamscale
 * (`tia-mode=teamscale-upload`).
 */
open class TestRun internal constructor(private val api: ITestwiseCoverageAgentApi) {
	/**
	 * Represents the result of running a single test.
	 */
	class TestResultWithMessage(
		/** Whether the test succeeded or failed.  */
		val result: ETestExecutionResult,
		/** An optional message, e.g. a stack trace in case of test failures.  */
		val message: String?
	)

	/**
	 * Informs the testwise coverage agent that a new test is about to start.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. In
	 * this case, the agent probably doesn't know that this test case was
	 * started, so its coverage is lost. This method already retries the request
	 * once, so this is likely a terminal failure. The caller should log this
	 * problem appropriately. Coverage for subsequent test cases could, however,
	 * potentially still be recorded. Thus, the caller should continue with test
	 * execution and continue informing the coverage agent about further test
	 * start and end events.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun startTest(uniformPath: String): RunningTest {
		handleRequestError(
			"Failed to start coverage recording for test case $uniformPath"
		) { api.testStarted(uniformPath.encodeUrl()) }
		return RunningTest(uniformPath, api)
	}

	/**
	 * Informs the testwise coverage agent that the caller has finished running all tests and should upload coverage to
	 * Teamscale. Only call this if you configured the agent to upload coverage to Teamscale
	 * (`tia-mode=teamscale-upload`). Otherwise, this method will throw an exception.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 * method already retries the request once, so this is likely a terminal
	 * failure. The recorded coverage is likely lost. The caller should log this
	 * problem appropriately.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun endTestRun(partial: Boolean) {
		handleRequestError(
			"Failed to create a coverage report and upload it to Teamscale. The coverage is most likely lost"
		) { api.testRunFinished(partial) }
	}
}
