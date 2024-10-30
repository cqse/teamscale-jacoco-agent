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
	private val testDetailsMap: MutableMap<String, TestDetails> = HashMap()

	/** Maps uniform paths to test executions.  */
	private val testExecutionsMap: MutableMap<String?, TestExecution> = HashMap()

	/** Holds all uniform paths for tests that have been written to the outputFile.  */
	private val processedTestUniformPaths: MutableSet<String?> = HashSet()

	init {
		for (testDetail: TestDetails in testDetails) {
			testDetailsMap.put(testDetail.uniformPath, testDetail)
		}
		for (testExecution: TestExecution in testExecutions) {
			testExecutionsMap.put(testExecution.uniformPath, testExecution)
		}
	}

	/**
	 * Converts the given [TestCoverageBuilder] to a [TestInfo] using the internally stored test details and
	 * test executions.
	 */
	fun createFor(testCoverageBuilder: TestCoverageBuilder): TestInfo {
		val resolvedUniformPath: String = resolveUniformPath(testCoverageBuilder.uniformPath)
		processedTestUniformPaths.add(resolvedUniformPath)

		val container: TestInfoBuilder = TestInfoBuilder(resolvedUniformPath)
		container.setCoverage(testCoverageBuilder)
		val testDetails: TestDetails? = testDetailsMap.get(resolvedUniformPath)
		if (testDetails == null) {
			System.err.println("No test details found for " + resolvedUniformPath)
		}
		container.setDetails(testDetails)
		val execution: TestExecution? = testExecutionsMap.get(resolvedUniformPath)
		if (execution == null) {
			System.err.println("No test execution found for " + resolvedUniformPath)
		}
		container.setExecution(execution)
		return container.build()
	}

	/** Returns [TestInfo]s for all tests that have not been used yet in [.createFor].  */
	fun createTestInfosWithoutCoverage(): List<TestInfo> {
		val results: ArrayList<TestInfo> = ArrayList()
		for (testDetails: TestDetails in testDetailsMap.values) {
			if (!processedTestUniformPaths.contains(testDetails.uniformPath)) {
				val testInfo: TestInfoBuilder = TestInfoBuilder(testDetails.uniformPath)
				testInfo.setDetails(testDetails)
				testInfo.setExecution(testExecutionsMap.get(testDetails.uniformPath))
				results.add(testInfo.build())
				processedTestUniformPaths.add(testDetails.uniformPath)
			}
		}
		for (testExecution: TestExecution in testExecutionsMap.values) {
			if (!processedTestUniformPaths.contains(testExecution.uniformPath)) {
				System.err.println(
					"Test " + testExecution.uniformPath + " was executed but no coverage was found. " +
							"Please make sure that you did provide all relevant exec files and that the test IDs passed to " +
							"the agent match the ones from the provided test execution list."
				)
				processedTestUniformPaths.add(testExecution.uniformPath)
			}
		}
		return results
	}

	/**
	 * Strips parameterized test arguments when the full path given in the coverage file cannot be found in the test
	 * details.
	 */
	private fun resolveUniformPath(originalUniformPath: String?): String {
		var uniformPath: String = originalUniformPath!!
		val testDetails: TestDetails? = testDetailsMap.get(uniformPath)
		if (testDetails == null) {
			uniformPath = TestwiseCoverageReportBuilder.Companion.stripParameterizedTestArguments(uniformPath)
		}
		return uniformPath
	}
}
