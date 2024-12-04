package com.teamscale.test_impacted.engine.executor

import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.test_impacted.commons.LoggerUtils.createLogger
import com.teamscale.tia.client.ITestwiseCoverageAgentApi
import com.teamscale.tia.client.UrlUtils.encodeUrl
import java.io.IOException
import java.util.logging.Level

/** Communicates test start and end to the agent and the end of the overall test execution.  */
open class TeamscaleAgentNotifier(
	/** A list of API services to signal test start and end to the agent.  */
	private val testwiseCoverageAgentApis: List<ITestwiseCoverageAgentApi>,
	/**
	 * Whether only a part of the tests is being executed (`true`) or whether all tests are executed
	 * (`false`).
	 */
	private val partial: Boolean
) {
	private val logger = createLogger()

	/** Reports the start of a test to the Teamscale JaCoCo agent.  */
	open fun startTest(testUniformPath: String) {
		try {
			testwiseCoverageAgentApis.forEach { apiService ->
				apiService.testStarted(testUniformPath.encodeUrl()).execute()
			}
		} catch (e: IOException) {
			logger.log(
				Level.SEVERE, e
			) { "Error while calling service api." }
		}
	}

	/** Reports the end of a test to the Teamscale JaCoCo agent.  */
	open fun endTest(testUniformPath: String, testExecution: TestExecution?) {
		try {
			testwiseCoverageAgentApis.forEach { apiService ->
				val url = testUniformPath.encodeUrl()
				if (testExecution == null) {
					apiService.testFinished(url).execute()
				} else {
					apiService.testFinished(url, testExecution).execute()
				}
			}
		} catch (e: IOException) {
			logger.log(
				Level.SEVERE, e
			) { "Error contacting test wise coverage agent." }
		}
	}

	/** Reports the end of the test run to the Teamscale JaCoCo agent.  */
	open fun testRunEnded() {
		try {
			testwiseCoverageAgentApis.forEach { apiService ->
				apiService.testRunFinished(partial).execute()
			}
		} catch (e: IOException) {
			logger.log(
				Level.SEVERE, e
			) { "Error contacting test wise coverage agent." }
		}
	}
}
