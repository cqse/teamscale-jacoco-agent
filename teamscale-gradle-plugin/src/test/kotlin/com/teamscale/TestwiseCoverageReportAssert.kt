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
        val actualCoveredLines =
            test.paths.single { it.path == path }.files.single { it.fileName == fileName }.coveredLines
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