package com.teamscale.jacoco.agent.convert

import com.teamscale.client.TestDetails
import com.teamscale.jacoco.util.Benchmark
import com.teamscale.jacoco.util.LoggingUtils
import com.teamscale.report.ReportUtils
import com.teamscale.report.jacoco.JaCoCoXmlReportGenerator
import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.testwise.ETestArtifactFormat
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder
import com.teamscale.report.util.AntPatternIncludeFilter
import com.teamscale.report.util.ILogger
import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfo
import org.jacoco.core.tools.ExecFileLoader
import org.slf4j.Logger

import java.io.File
import java.io.IOException

import com.teamscale.jacoco.util.LoggingUtils.wrap

/** Converts one .exec binary coverage file to XML.  */
class Converter
/** Constructor.  */
    (
    /** The command line arguments.  */
    private val arguments: ConvertCommand
) {

    /** Converts one .exec binary coverage file to XML.  */
    @Throws(IOException::class)
    fun runJaCoCoReportGeneration() {
        val jacocoExecutionDataList = ReportUtils
            .listFiles(ETestArtifactFormat.JACOCO, arguments.getInputFiles())

        val loader = ExecFileLoader()
        for (jacocoExecutionData in jacocoExecutionDataList) {
            loader.load(jacocoExecutionData)
        }

        val sessionInfo = loader.sessionInfoStore.getMerged("merged")
        val executionDataStore = loader.executionDataStore

        val locationIncludeFilter = AntPatternIncludeFilter(
            arguments.locationIncludeFilters, arguments.locationExcludeFilters
        )
        val logger = LoggingUtils.getLogger(this)
        val generator = JaCoCoXmlReportGenerator(
            arguments.getClassDirectoriesOrZips(),
            locationIncludeFilter, arguments.shouldIgnoreDuplicateClassFiles(), wrap(logger)
        )

        Benchmark("Generating the XML report").use { benchmark ->
            val xml = generator.convert(Dump(sessionInfo, executionDataStore))
            FileSystemUtils.writeFileUTF8(arguments.getOutputFile(), xml)
        }
    }

    /** Converts one .exec binary coverage file, test details and test execution files to JSON testwise coverage.  */
    @Throws(IOException::class, CoverageGenerationException::class)
    fun runTestwiseCoverageReportGeneration() {
        val testDetails = ReportUtils.readObjects(
            ETestArtifactFormat.TEST_LIST,
            Array<TestDetails>::class.java, arguments.getInputFiles()
        )
        val testExecutions = ReportUtils.readObjects(
            ETestArtifactFormat.TEST_EXECUTION,
            Array<TestExecution>::class.java, arguments.getInputFiles()
        )

        val jacocoExecutionDataList = ReportUtils
            .listFiles(ETestArtifactFormat.JACOCO, arguments.getInputFiles())
        val logger = CommandLineLogger()
        val includeFilter = AntPatternIncludeFilter(
            arguments.locationIncludeFilters,
            arguments.locationExcludeFilters
        )
        val generator = JaCoCoTestwiseReportGenerator(
            arguments.getClassDirectoriesOrZips(),
            includeFilter,
            true,
            logger
        )

        Benchmark("Generating the testwise coverage report").use { benchmark ->
            val coverage = generator.convert(jacocoExecutionDataList)
            logger.info(
                "Merging report with " + testDetails.size + " Details/" + coverage.getTests()
                    .size + " Coverage/" + testExecutions.size + " Results"
            )

            val report = TestwiseCoverageReportBuilder
                .createFrom(testDetails, coverage.getTests(), testExecutions)
            ReportUtils.writeReportToFile(arguments.getOutputFile(), report)
        }
    }
}
