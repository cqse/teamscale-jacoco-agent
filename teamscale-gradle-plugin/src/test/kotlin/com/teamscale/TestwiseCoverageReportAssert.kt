package com.teamscale

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import org.assertj.core.api.AbstractAssert
import java.io.File

class TestwiseCoverageReportAssert(actual: TestwiseCoverageReport) :
	AbstractAssert<TestwiseCoverageReportAssert, TestwiseCoverageReport>(
		actual,
		TestwiseCoverageReportAssert::class.java
	) {

	/** Asserts that the report has the partial flag set to the given value. */
	fun hasPartial(expectedPartial: Boolean): TestwiseCoverageReportAssert {
		isNotNull

		if (actual.partial != expectedPartial) {
			failWithMessage(
				"Expected partial flag to be <%s> but was <%s>",
				expectedPartial,
				actual.partial
			)
		}

		return this
	}

	fun containsExecutionResult(testUniformPath: String, result: ETestExecutionResult): TestwiseCoverageReportAssert {
		isNotNull

		val test = getTest(testUniformPath)
		if (test.result != result) {
			failWithMessage(
				"Expected test execution result of %s to be <%s> but was <%s>",
				testUniformPath,
				result,
				test.result
			)
		}

		return this
	}

	private fun getTest(test: String): TestInfo {
		return actual.tests.single { it.uniformPath == test }
	}

	/** Asserts that the given number of tests have non-empty coverage */
	fun hasTestsWithCoverage(size: Int): TestwiseCoverageReportAssert {
		isNotNull

		if (actual.tests.count { it.paths.isNotEmpty() } != size) {
			failWithMessage("Expected <%s> test(s) with coverage but got <%s>", size, actual.tests.size)
		}

		return this
	}

	fun hasSize(size: Int): TestwiseCoverageReportAssert {
		isNotNull

		if (actual.tests.size != size) {
			failWithMessage("Expected <%s> test(s) but got <%s>", size, actual.tests.size)
		}

		return this
	}

	fun containsCoverage(
		testUniformPath: String,
		filePath: String,
		expectedCoveredLines: String
	): TestwiseCoverageReportAssert {
		isNotNull

		val path = File(filePath).parent
		val fileName = File(filePath).name

		val test = getTest(testUniformPath)
		val pathCoverage = test.paths.find { it.path == path }
		if (pathCoverage == null) {
			failWithMessage("Expected %s to cover path %s but it did not", testUniformPath, path)
			return this
		}

		val fileCoverage = pathCoverage.files.find { it.fileName == fileName }
		if (fileCoverage == null) {
			failWithMessage("Expected %s to cover file %s but it did not", testUniformPath, filePath)
			return this
		}

		val actualCoveredLines = fileCoverage.coveredLines
		if (actualCoveredLines != expectedCoveredLines) {
			failWithMessage(
				"Expected %s to cover <%s> but was <%s>",
				testUniformPath,
				expectedCoveredLines,
				actualCoveredLines
			)
		}

		return this
	}

	companion object {
		fun assertThat(actual: TestwiseCoverageReport): TestwiseCoverageReportAssert {
			return TestwiseCoverageReportAssert(actual)
		}
	}
}