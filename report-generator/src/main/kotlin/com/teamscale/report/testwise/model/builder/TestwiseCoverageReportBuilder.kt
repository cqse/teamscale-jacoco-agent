package com.teamscale.report.testwise.model.builder

import com.teamscale.client.TestDetails
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import java.util.function.Function

/** Container for coverage produced by multiple tests.  */
class TestwiseCoverageReportBuilder {
	/** A mapping from test ID to [TestCoverageBuilder].  */
	private val tests = mutableMapOf<String, TestInfoBuilder>()

	private fun build(partial: Boolean): TestwiseCoverageReport {
		val report = TestwiseCoverageReport(partial)
		tests.values
			.sortedBy { it.uniformPath }
			.map { it.build() }
			.forEach { report.tests.add(it) }
		return report
	}

	companion object {
		/**
		 * Adds the [TestCoverageBuilder] to the map. If there is already a test with the same ID the coverage is
		 * merged.
		 */
		@JvmStatic
		fun createFrom(
			testDetailsList: Collection<TestDetails>,
			testCoverage: Collection<TestCoverageBuilder>,
			testExecutions: Collection<TestExecution>,
			partial: Boolean
		): TestwiseCoverageReport {
			val report = TestwiseCoverageReportBuilder()
			for (testDetails: TestDetails in testDetailsList) {
				val container = TestInfoBuilder(testDetails.uniformPath)
				container.setDetails(testDetails)
				report.tests[testDetails.uniformPath] = container
			}
			for (coverage: TestCoverageBuilder in testCoverage) {
				val container = resolveUniformPath(report, coverage.uniformPath) ?: continue
				container.setCoverage(coverage)
			}
			for (testExecution: TestExecution in testExecutions) {
				val path = testExecution.uniformPath ?: continue
				val container = resolveUniformPath(report, path) ?: continue
				container.setExecution(testExecution)
			}
			return report.build(partial)
		}

		private fun resolveUniformPath(report: TestwiseCoverageReportBuilder, uniformPath: String): TestInfoBuilder? {
			val container = report.tests[uniformPath]
			if (container != null) {
				return container
			}
			val shortenedUniformPath: String = stripParameterizedTestArguments(uniformPath)
			val testInfoBuilder = report.tests[shortenedUniformPath]
			if (testInfoBuilder == null) {
				System.err.println("No container found for test '$uniformPath'!")
			}
			return testInfoBuilder
		}

		/**
		 * Removes parameterized test arguments from the given uniform path.
		 */
		fun stripParameterizedTestArguments(uniformPath: String): String {
			return uniformPath.replaceFirst("(.*\\))\\[.*]".toRegex(), "$1")
		}
	}
}
