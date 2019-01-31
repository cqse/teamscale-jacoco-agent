package com.teamscale.report.testwise.jacoco

import com.teamscale.client.TestDetails
import com.teamscale.report.ReportUtils
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder
import com.teamscale.report.util.AntPatternIncludeFilter
import com.teamscale.report.util.ILogger
import org.assertj.core.api.Assertions
import org.conqat.lib.commons.collections.CollectionUtils.emptyList
import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.conqat.lib.commons.test.CCSMTestCaseBase
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.*

/** Tests for the [JaCoCoTestwiseReportGenerator] class.  */
class JaCoCoTestwiseReportGeneratorTest : CCSMTestCaseBase() {

    /** Tests that the [JaCoCoTestwiseReportGenerator] produces the expected output.  */
    @Test
    @Throws(Exception::class)
    fun testSmokeTestTestwiseReportGeneration() {
        val report = runGenerator("jacoco/cqddl/classes.zip", "jacoco/cqddl/coverage.exec")
        val expected = FileSystemUtils.readFileUTF8(useTestFile("jacoco/cqddl/report.json.expected"))
        Assertions.assertThat(report).isEqualTo(expected)
    }

    /** Tests that the [JaCoCoTestwiseReportGenerator] produces the expected output.  */
    @Test
    @Throws(Exception::class)
    fun testSampleTestwiseReportGeneration() {
        val report = runGenerator("jacoco/sample/classes.zip", "jacoco/sample/coverage.exec")
        val expected = FileSystemUtils.readFileUTF8(useTestFile("jacoco/sample/report.json.expected"))
        Assertions.assertThat(report).isEqualTo(expected)
    }

    /** Runs the report generator.  */
    @Throws(Exception::class)
    private fun runGenerator(testDataFolder: String, execFileName: String): String {
        val classFileFolder = useTestFile(testDataFolder)
        val includeFilter = AntPatternIncludeFilter(emptyList(), emptyList())
        val testwiseCoverage = JaCoCoTestwiseReportGenerator(
            listOf(classFileFolder),
            includeFilter, true,
            mock(ILogger::class.java)
        ).convert(useTestFile(execFileName))
        return ReportUtils.getReportAsString<Any>(generateDummyReportFrom(testwiseCoverage))
    }

    companion object {

        /** Generates a dummy coverage report object that wraps the given [TestwiseCoverage].  */
        fun generateDummyReportFrom(testwiseCoverage: TestwiseCoverage): TestwiseCoverageReport {
            val testDetails = ArrayList<TestDetails>()
            for (test in testwiseCoverage.getTests()) {
                testDetails.add(TestDetails(test.uniformPath, "/path/to/source", "content"))
            }
            val testExecutions = ArrayList<TestExecution>()
            for (test in testwiseCoverage.getTests()) {
                testExecutions.add(
                    TestExecution(
                        test.uniformPath, test.uniformPath.length.toLong(),
                        ETestExecutionResult.PASSED
                    )
                )
            }
            return TestwiseCoverageReportBuilder
                .createFrom(testDetails, testwiseCoverage.getTests(), testExecutions)
        }
    }
}