package com.teamscale.tia.client

import com.teamscale.client.utils.JsonUtils.deserialize
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.tia.client.TestRun.TestResultWithMessage
import okhttp3.ResponseBody
import java.io.IOException

class RunningTest(private val uniformPath: String, private val api: ITestwiseCoverageAgentApi) {
	private class AgentConfigurationMismatch(message: String) : RuntimeException(message)

	/**
	 * Signals to the agent that the test runner has finished executing this test and the result of the test run.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 * method already retries the request once, so this is likely a terminal
	 * failure. The caller should record this problem appropriately. Coverage
	 * for subsequent test cases could, however, potentially still be recorded.
	 * Thus, the caller should continue with test execution and continue
	 * informing the coverage agent about further test start and end events.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun endTest(result: TestResultWithMessage) {
		// the agent already records test duration, so we can simply provide a dummy value here
		val execution = TestExecution(
			uniformPath, 0L, result.result,
			result.message
		)
		AgentCommunicationUtils.handleRequestError(
				{
					api.testFinished(
						UrlUtils.percentEncode(
							uniformPath
						), execution
					)
				},
				"Failed to end coverage recording for test case " + uniformPath +
						". Coverage for that test case is most likely lost."
			)?.let { body ->
			if (body.readBodyString().isNotBlank()) {
				throw AgentConfigurationMismatch(
					("The agent seems to be configured to return test coverage via" +
							" HTTP to the tia-client (agent option `tia-mode=http`) but you did not instruct the" +
							" tia-client to handle this. Please either reconfigure the agent or call" +
							" #endTestAndRetrieveCoverage() instead of this method and handle the returned coverage." +
							" As it is currently configured, the agent will not store or process the recorded coverage" +
							" in any way other than sending it to the tia-client via HTTP so it is lost permanently.")
				)
			}
		}


	}

	@Throws(AgentHttpRequestFailedException::class)
	private fun ResponseBody.readBodyString(): String {
		try {
			return string()
		} catch (e: IOException) {
			throw AgentHttpRequestFailedException("Unable to read agent HTTP response body string", e)
		}
	}

	/**
	 * Signals to the agent that the test runner has finished executing this test and the result of the test run. It
	 * will also parse the testwise coverage data returned by the agent for this test and return it so it can be
	 * manually processed by you. The agent will not store or otherwise process this coverage, so be sure to do so
	 * yourself.
	 *
	 *
	 * This method assumes that the agent is configured to return each test's coverage data via HTTP. It will receive
	 * and parse the data. If the agent is configured differently, this method will throw a terminal
	 * [RuntimeException]. In this case, you must reconfigure the agent with the `tia-mode=http` option enabled.
	 *
	 * @throws AgentHttpRequestFailedException if communicating with the agent fails or in case of internal errors. This
	 * method already retries the request once, so this is likely a terminal
	 * failure. The recorded coverage is likely lost. The caller should log this
	 * problem appropriately.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun endTestAndRetrieveCoverage(
		result: TestResultWithMessage
	): TestInfo {
		// the agent already records test duration, so we can simply provide a dummy value here
		val execution = TestExecution(
			uniformPath, 0L, result.result,
			result.message
		)
		val body = AgentCommunicationUtils.handleRequestError(
			{
				api.testFinished(
					UrlUtils.percentEncode(
						uniformPath
					), execution
				)
			},
			("Failed to end coverage recording for test case " + uniformPath +
					". Coverage for that test case is most likely lost.")
		)

		val json = body.readBodyString()
		if (json.isBlank()) {
			throw AgentConfigurationMismatch(
				"You asked the tia-client to retrieve this test's coverage via HTTP" +
						" but the agent is not configured for this. Please reconfigure the agent to use `tia-mode=http`."
			)
		}

		try {
			return json.deserialize<TestInfo>()
		} catch (e: IOException) {
			throw AgentHttpRequestFailedException(
				("Unable to parse the JSON returned by the agent. Maybe you have" +
						" a version mismatch between the tia-client and the agent?. Json returned by the agent: `" + json +
						"`"), e
			)
		}
	}
}