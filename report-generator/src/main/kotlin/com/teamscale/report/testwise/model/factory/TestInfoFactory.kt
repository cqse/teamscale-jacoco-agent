package com.teamscale.report.testwise.model.factory

import com.teamscale.client.TestDetails
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder
import com.teamscale.report.testwise.model.builder.TestInfoBuilder
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder

/**
 * Factory class for converting [TestCoverageBuilder] to [TestInfo]s while augmenting them with information
 * from test details and test executions.
 */
class TestInfoFactory(testDetails: List<TestDetails>, testExecutions: List<TestExecution>) {
	/** Maps uniform paths to test details.  */
	private val testDetailsMap = mutableMapOf<String, TestDetails>()

	/** Maps uniform paths to test executions.  */
	private val testExecutionsMap = mutableMapOf<String, TestExecution>()

	/** Holds all uniform paths for tests that have been written to the outputFile.  */
	private val processedTestUniformPaths = mutableSetOf<String>()

	init {
		testDetails.forEach { testDetail ->
			testDetailsMap[testDetail.uniformPath] = testDetail
		}
		testExecutions.forEach { testExecution ->
			testExecution.uniformPath?.let {
				testExecutionsMap[it] = testExecution
			}
		}
	}

	/**
	 * Converts the given [TestCoverageBuilder] to a [TestInfo] using the internally stored test details and
	 * test executions.
	 */
	fun createFor(testCoverageBuilder: TestCoverageBuilder): TestInfo {
		val resolvedUniformPath = testCoverageBuilder.uniformPath.resolveUniformPath()
		processedTestUniformPaths.add(resolvedUniformPath)

		return TestInfoBuilder(resolvedUniformPath).apply {
			setCoverage(testCoverageBuilder)
			testDetailsMap[resolvedUniformPath]?.let { testDetails ->
				setDetails(testDetails)
			} ?: System.err.println("No test details found for $resolvedUniformPath")
			testExecutionsMap[resolvedUniformPath]?.let { execution ->
				setExecution(execution)
			} ?: System.err.println("No test execution found for $resolvedUniformPath")
		}.build()
	}

	/** Returns [TestInfo]s for all tests that have not been used yet in [.createFor].  */
	fun createTestInfosWithoutCoverage(): List<TestInfo> {
		val results = testDetailsMap.values.mapNotNull { testDetails ->
			if (processedTestUniformPaths.contains(testDetails.uniformPath)) return@mapNotNull null

			processedTestUniformPaths.add(testDetails.uniformPath)
			TestInfoBuilder(testDetails.uniformPath).apply {
				setDetails(testDetails)
				testExecutionsMap[testDetails.uniformPath]?.let { setExecution(it) }
			}.build()
		}
		testExecutionsMap.values.forEach { testExecution ->
			if (processedTestUniformPaths.contains(testExecution.uniformPath)) return@forEach
			System.err.println(
				"Test " + testExecution.uniformPath + " was executed but no coverage was found. " +
						"Please make sure that you did provide all relevant exec files and that the test IDs passed to " +
						"the agent match the ones from the provided test execution list."
			)
			testExecution.uniformPath?.let { processedTestUniformPaths.add(it) }
		}
		return results
	}

	/**
	 * Strips parameterized test arguments when the full path given in the coverage file cannot be found in the test
	 * details.
	 */
	private fun String.resolveUniformPath() =
		testDetailsMap[this]?.uniformPath ?: TestwiseCoverageReportBuilder.stripParameterizedTestArguments(this)
}
