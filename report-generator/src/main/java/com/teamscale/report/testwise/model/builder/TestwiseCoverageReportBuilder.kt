package com.teamscale.report.testwise.model.builder

import com.teamscale.client.TestDetails
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import org.conqat.lib.commons.collections.CollectionUtils
import java.util.Comparator
import java.util.HashMap

/** Container for coverage produced by multiple tests.  */
class TestwiseCoverageReportBuilder {

    /** A mapping from test ID to [TestCoverageBuilder].  */
    private val tests = HashMap<String, TestInfoBuilder>()

    private fun build(): TestwiseCoverageReport {
        val report = TestwiseCoverageReport()
        val testInfoBuilders = CollectionUtils
            .sort(
                tests.values,
                Comparator.comparing<TestInfoBuilder, String>(Function<TestInfoBuilder, String> { it.getUniformPath() })
            )
        for (testInfoBuilder in testInfoBuilders) {
            val testInfo = testInfoBuilder.build()
            if (testInfo == null) {
                System.err.println("No coverage for test '" + testInfoBuilder.uniformPath + "'")
                continue
            }
            report.tests.add(testInfo)
        }
        return report
    }

    companion object {

        /**
         * Adds the [TestCoverageBuilder] to the map.
         * If there is already a test with the same ID the coverage is merged.
         */
        fun createFrom(
            testDetailsList: Collection<TestDetails>,
            testCoverage: Collection<TestCoverageBuilder>,
            testExecutions: Collection<TestExecution>
        ): TestwiseCoverageReport {
            val report = TestwiseCoverageReportBuilder()
            for (testDetails in testDetailsList) {
                val container = TestInfoBuilder(testDetails.uniformPath)
                container.setDetails(testDetails)
                report.tests[testDetails.uniformPath] = container
            }
            for (coverage in testCoverage) {
                val container = resolveUniformPath(report, coverage.uniformPath) ?: continue
                container.setCoverage(coverage)
            }
            for (testExecution in testExecutions) {
                val container = resolveUniformPath(report, testExecution.uniformPath) ?: continue
                container.setExecution(testExecution)
            }
            return report.build()
        }

        private fun resolveUniformPath(report: TestwiseCoverageReportBuilder, uniformPath: String): TestInfoBuilder? {
            val container = report.tests[uniformPath]
            if (container != null) {
                return container
            }
            val shortenedUniformPath = uniformPath.replaceFirst("(.*\\))\\[.*]".toRegex(), "$1")
            val testInfoBuilder = report.tests[shortenedUniformPath]
            if (testInfoBuilder == null) {
                System.err.println("No container found for test '$uniformPath'!")
            }
            return testInfoBuilder
        }
    }
}
