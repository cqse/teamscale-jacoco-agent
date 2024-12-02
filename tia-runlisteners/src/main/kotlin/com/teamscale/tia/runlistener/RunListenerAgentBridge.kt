package com.teamscale.tia.runlistener

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.tia.client.RunningTest
import com.teamscale.tia.client.TestRun
import com.teamscale.tia.client.TestRun.TestResultWithMessage
import com.teamscale.tia.client.TiaAgent
import okhttp3.HttpUrl

/**
 * Handles communication with the [TiaAgent] and logging for any type of test run listener.
 * This allows, e.g., Junit 4 and Junit 5 listeners to share the same logic for these tasks.
 */
class RunListenerAgentBridge(runListenerClassName: String) {
	private val testRun: TestRun
	private var runningTest: RunningTest? = null
	private val logger = RunListenerLogger.create<RunListenerAgentBridge>()

	private class RunListenerConfigurationException(message: String) : RuntimeException(message)

	init {
		logger.debug("$runListenerClassName instantiated")
		val agentUrl = System.getProperty("tia.agent") ?: System.getenv("TIA_AGENT")
		if (agentUrl == null) {
			val exception = RunListenerConfigurationException(
				"You did not provide the URL of a Teamscale JaCoCo agent that will record test-wise coverage." +
						" You can configure the URL either as a system property with -Dtia.agent=URL" +
						" or as an environment variable with TIA_AGENT=URL."
			)
			logger.error("Failed to instantiate $runListenerClassName", exception)
			throw exception
		}

		val agent = TiaAgent(false, HttpUrl.get(agentUrl))
		testRun = agent.startTestRunWithoutTestSelection()
	}

	private fun handleErrors(description: String, action: () -> Unit) {
		runCatching { action() }.onFailure { e ->
			logger.error("Encountered an error while recording test-wise coverage in step: $description", e)
		}
	}

	/** Notifies the [TiaAgent] that the given test was started.  */
	fun testStarted(uniformPath: String) {
		logger.debug("Started test '$uniformPath'")
		handleErrors("Starting test '$uniformPath'") {
			runningTest = testRun.startTest(uniformPath)
		}
	}

	/**
	 * Notifies the [TiaAgent] that the given test was finished (both successfully and unsuccessfully).
	 *
	 * @param message may be null if no useful message can be provided.
	 */
	@JvmOverloads
	fun testFinished(uniformPath: String, result: ETestExecutionResult, message: String? = null) {
		logger.debug("Finished test '$uniformPath'")
		handleErrors("Finishing test '$uniformPath'") {
			runningTest?.endTest(TestResultWithMessage(result, message))
			runningTest = null
		}
	}

	/**
	 * Notifies the [TiaAgent] that the given test was skipped.
	 *
	 * @param reason Optional reason. Pass null if no reason was provided by the test framework.
	 */
	fun testSkipped(uniformPath: String, reason: String?) {
		logger.debug("Skipped test '$uniformPath'")
		handleErrors("Skipping test '$uniformPath'") {
			runningTest?.endTest(TestResultWithMessage(ETestExecutionResult.SKIPPED, reason))
			runningTest = null
		}
	}

	/**
	 * Notifies the [TiaAgent] that the whole test run is finished and that test-wise coverage recording can end
	 * now.
	 */
	fun testRunFinished() {
		logger.debug("Finished test run")
		handleErrors("Finishing the test run") {
			testRun.endTestRun(false)
		}
	}

	companion object {
		inline fun <reified T> create() = RunListenerAgentBridge(T::class.java.name)
	}
}