package com.teamscale.tia.runlistener

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.tia.client.TiaAgent
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.util.*
import java.util.function.Function

/**
 * [TestExecutionListener] that uses the [TiaAgent] to record test-wise coverage.
 */
class JUnit5TestwiseCoverageExecutionListener : TestExecutionListener {
	private val bridge = RunListenerAgentBridge.create<JUnit5TestwiseCoverageExecutionListener>()

	override fun executionStarted(testIdentifier: TestIdentifier) {
		if (!testIdentifier.isTest) return
		val uniformPath = getUniformPath(testIdentifier)
		bridge.testStarted(uniformPath)
	}

	private fun getUniformPath(testIdentifier: TestIdentifier) =
		testIdentifier.source.flatMap { source ->
			parseTestSource(source)
		}.orElse(testIdentifier.displayName)

	private fun parseTestSource(source: TestSource) =
		when (source) {
			is ClassSource -> Optional.of(source.className.replace('.', '/'))
			is MethodSource -> Optional.of(
				source.className.replace('.', '/') + "/" +
						source.methodName + "(" + source.methodParameterTypes + ")"
			)
			else -> Optional.empty()
		}

	override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
		if (!testIdentifier.isTest) {
			return
		}
		val uniformPath = getUniformPath(testIdentifier)
		val result = when (testExecutionResult.status) {
			TestExecutionResult.Status.SUCCESSFUL -> ETestExecutionResult.PASSED
			TestExecutionResult.Status.ABORTED -> ETestExecutionResult.ERROR
			TestExecutionResult.Status.FAILED -> ETestExecutionResult.FAILURE
			else -> ETestExecutionResult.FAILURE
		}

		bridge.testFinished(uniformPath, result)
	}

	override fun executionSkipped(testIdentifier: TestIdentifier, reason: String) {
		if (testIdentifier.isContainer) return
		val uniformPath = getUniformPath(testIdentifier)
		bridge.testSkipped(uniformPath, reason)
	}

	override fun testPlanExecutionFinished(testPlan: TestPlan) {
		bridge.testRunFinished()
	}
}
