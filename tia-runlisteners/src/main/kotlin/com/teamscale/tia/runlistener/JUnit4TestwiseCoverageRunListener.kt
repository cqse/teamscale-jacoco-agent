package com.teamscale.tia.runlistener

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.tia.client.TiaAgent
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

/**
 * [RunListener] that uses the [TiaAgent] to record test-wise coverage.
 */
class JUnit4TestwiseCoverageRunListener : RunListener() {
	private val bridge = RunListenerAgentBridge.create<JUnit4TestwiseCoverageRunListener>()

	override fun testStarted(description: Description) {
		val uniformPath = getUniformPath(description)
		bridge.testStarted(uniformPath)
	}

	private fun getUniformPath(description: Description): String {
		var uniformPath = description.className.replace('.', '/')
		if (description.methodName != null) {
			uniformPath += "/" + description.methodName
		}
		return uniformPath
	}

	override fun testFinished(description: Description) {
		val uniformPath = getUniformPath(description)
		bridge.testFinished(uniformPath, ETestExecutionResult.PASSED)
	}

	override fun testFailure(failure: Failure) {
		val uniformPath = getUniformPath(failure.description)
		bridge.testFinished(uniformPath, ETestExecutionResult.FAILURE, failure.message)
	}

	override fun testAssumptionFailure(failure: Failure) {
		val uniformPath = getUniformPath(failure.description)
		bridge.testFinished(uniformPath, ETestExecutionResult.FAILURE)
	}

	override fun testIgnored(description: Description) {
		val uniformPath = getUniformPath(description)
		bridge.testSkipped(uniformPath, null)
	}

	override fun testRunFinished(result: Result) {
		bridge.testRunFinished()
	}
}
