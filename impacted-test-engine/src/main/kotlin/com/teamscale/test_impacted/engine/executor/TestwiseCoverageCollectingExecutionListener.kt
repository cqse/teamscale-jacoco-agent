package com.teamscale.test_impacted.engine.executor

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.test_impacted.commons.LoggerUtils.createLogger
import com.teamscale.test_impacted.test_descriptor.ITestDescriptorResolver
import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils.isRepresentative
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.reporting.ReportEntry
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

/**
 * An execution listener which delegates events to another [EngineExecutionListener] and notifies Teamscale agents
 * collecting test wise coverage.
 */
class TestwiseCoverageCollectingExecutionListener(
	/** An API to signal test start and end to the agent.  */
	private val teamscaleAgentNotifier: TeamscaleAgentNotifier,
	private val testDescriptorResolver: ITestDescriptorResolver,
	private val delegateEngineExecutionListener: EngineExecutionListener
) : EngineExecutionListener {
	companion object {
		private val LOG = createLogger()
	}

	/** List of tests that have been executed, skipped or failed.  */
	val testExecutions = mutableListOf<TestExecution>()

	/** Time when the current test execution started.  */
	private var executionStartTime = 0L

	private val testResultCache = mutableMapOf<UniqueId, MutableList<TestExecutionResult>>()

	override fun dynamicTestRegistered(testDescriptor: TestDescriptor) {
		delegateEngineExecutionListener.dynamicTestRegistered(testDescriptor)
	}

	override fun executionSkipped(testDescriptor: TestDescriptor, reason: String) {
		if (!testDescriptor.isRepresentative()) {
			delegateEngineExecutionListener.executionStarted(testDescriptor)
			testDescriptor.children.forEach { executionSkipped(it, reason) }
			delegateEngineExecutionListener.executionFinished(testDescriptor, TestExecutionResult.successful())
			return
		}

		testDescriptorResolver.getUniformPath(testDescriptor).ifPresent { testUniformPath ->
			testExecutions.add(
				TestExecution(
					testUniformPath,
					0L,
					ETestExecutionResult.SKIPPED,
					reason
				)
			)
			delegateEngineExecutionListener.executionSkipped(testDescriptor, reason)
		}
	}

	override fun executionStarted(testDescriptor: TestDescriptor) {
		if (testDescriptor.isRepresentative()) {
			testDescriptorResolver.getUniformPath(testDescriptor).ifPresent { testUniformPath ->
				teamscaleAgentNotifier.startTest(testUniformPath)
			}
			executionStartTime = System.currentTimeMillis()
		}
		delegateEngineExecutionListener.executionStarted(testDescriptor)
	}

	override fun executionFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
		if (testDescriptor.isRepresentative()) {
			val uniformPath = testDescriptorResolver.getUniformPath(testDescriptor)
			if (!uniformPath.isPresent) {
				return
			}

			val testExecution = getTestExecution(
				testDescriptor, testExecutionResult, uniformPath.get()
			)
			if (testExecution != null) {
				testExecutions.add(testExecution)
			}
			teamscaleAgentNotifier.endTest(uniformPath.get(), testExecution)
		} else if (testDescriptor.parent.isPresent) {
			val testExecutionResults = testResultCache.computeIfAbsent(
				testDescriptor.parent.get().uniqueId
			) { mutableListOf() }
			testExecutionResults.add(testExecutionResult)
		}

		delegateEngineExecutionListener.executionFinished(testDescriptor, testExecutionResult)
	}

	private fun getTestExecution(
		testDescriptor: TestDescriptor,
		testExecutionResult: TestExecutionResult,
		testUniformPath: String
	): TestExecution? {
		val testExecutionResults = getTestExecutionResults(testDescriptor, testExecutionResult)

		val executionEndTime = System.currentTimeMillis()
		val duration = executionEndTime - executionStartTime
		val message = StringBuilder()
		var status = TestExecutionResult.Status.SUCCESSFUL
		testExecutionResults.forEach { executionResult ->
			if (message.isNotEmpty()) {
				message.append("\n\n")
			}
			message.append(executionResult.throwable.buildStacktrace())
			// Aggregate status here to most severe status according to SUCCESSFUL < ABORTED < FAILED
			if (status.ordinal < executionResult.status.ordinal) {
				status = executionResult.status
			}
		}

		return buildTestExecution(testUniformPath, duration, status, message.toString())
	}

	private fun getTestExecutionResults(
		testDescriptor: TestDescriptor,
		testExecutionResult: TestExecutionResult
	): List<TestExecutionResult> {
		val testExecutionResults = mutableListOf<TestExecutionResult>()
		val childTestExecutionResult = testResultCache.remove(testDescriptor.uniqueId)
		if (childTestExecutionResult != null) {
			testExecutionResults.addAll(childTestExecutionResult)
		}
		testExecutionResults.add(testExecutionResult)
		return testExecutionResults
	}

	private fun buildTestExecution(
		testUniformPath: String,
		duration: Long,
		status: TestExecutionResult.Status,
		message: String
	): TestExecution? {
		when (status) {
			TestExecutionResult.Status.SUCCESSFUL -> return TestExecution(
				testUniformPath, duration, ETestExecutionResult.PASSED
			)

			TestExecutionResult.Status.ABORTED -> return TestExecution(
				testUniformPath, duration, ETestExecutionResult.ERROR, message
			)

			TestExecutionResult.Status.FAILED -> return TestExecution(
				testUniformPath, duration, ETestExecutionResult.FAILURE, message
			)

			else -> {
				LOG.severe { "Got unexpected test execution result status: $status" }
				return null
			}
		}
	}

	/** Extracts the stacktrace from the given [Throwable] into a string or returns null if no throwable is given.  */
	private fun Optional<Throwable>.buildStacktrace(): String? {
		if (!isPresent) {
			return null
		}

		val sw = StringWriter()
		val pw = PrintWriter(sw)
		get().printStackTrace(pw)
		return sw.toString()
	}

	override fun reportingEntryPublished(testDescriptor: TestDescriptor, entry: ReportEntry) {
		delegateEngineExecutionListener.reportingEntryPublished(testDescriptor, entry)
	}
}
