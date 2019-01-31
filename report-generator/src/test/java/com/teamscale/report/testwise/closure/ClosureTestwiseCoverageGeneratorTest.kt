package com.teamscale.report.testwise.closure

import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGeneratorTest
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.util.AntPatternIncludeFilter
import org.conqat.lib.commons.collections.CollectionUtils
import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.conqat.lib.commons.test.CCSMTestCaseBase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.io.File
import java.io.IOException
import java.util.Arrays
import java.util.Collections

import com.teamscale.report.ReportUtils.getReportAsString
import org.assertj.core.api.Assertions.assertThat

/** Tests for [ClosureTestwiseCoverageGenerator].  */
@RunWith(JUnit4::class)
class ClosureTestwiseCoverageGeneratorTest : CCSMTestCaseBase() {

    /** Tests that the JSON reports produce the expected result.  */
    @Test
    @Throws(IOException::class)
    fun testTestwiseReportGeneration() {
        val actual = runGenerator("closure")
        assertThat(actual)
            .isEqualToNormalizingWhitespace(FileSystemUtils.readFileUTF8(useTestFile("closure/report.json.expected")))
    }

    /** Runs the report generator.  */
    private fun runGenerator(closureCoverageFolder: String): String {
        val coverageFolder = useTestFile(closureCoverageFolder)
        val includeFilter = AntPatternIncludeFilter(
            CollectionUtils.emptyList<Any>(),
            Arrays.asList("**/google-closure-library/**", "**.soy.generated.js", "soyutils_usegoog.js")
        )
        val testwiseCoverage = ClosureTestwiseCoverageGenerator(
            listOf(coverageFolder), includeFilter
        )
            .readTestCoverage()
        return INSTANCE.getReportAsString(JaCoCoTestwiseReportGeneratorTest.generateDummyReportFrom(testwiseCoverage))
    }
}