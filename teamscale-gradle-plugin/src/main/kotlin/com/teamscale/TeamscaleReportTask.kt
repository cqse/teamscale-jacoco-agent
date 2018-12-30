package com.teamscale

import com.teamscale.client.TestDetails
import com.teamscale.config.GoogleClosureConfiguration
import com.teamscale.config.SerializableFilter
import com.teamscale.config.TeamscaleTaskExtension
import com.teamscale.report.ReportUtils
import com.teamscale.report.testwise.ETestArtifactFormat
import com.teamscale.report.testwise.closure.ClosureTestwiseCoverageGenerator
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder
import com.teamscale.report.util.ILogger
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Task which runs the impacted tests. */
open class TeamscaleReportTask : DefaultTask() {

    /**
     * Test task's name for which reports are generated.
     */
    lateinit var testTaskName: String

    /**
     * Reference to the configuration that should be used for this task.
     */
    lateinit var configuration: TeamscaleTaskExtension

    /* Task inputs */

    private val agentFilter: SerializableFilter
        @Input
        get() = configuration.agent.getFilter()

    private val closureIncludeFilter: GoogleClosureConfiguration.FileNameFilter
        @Input
        get() = configuration.report.googleClosureCoverage.getFilter()

    private val closureCoverageDir: Set<File>
        @InputFiles
        get() = configuration.report.googleClosureCoverage.destination ?: emptySet()

    /** Report files to included artifacts. */
    private val reportsToArtifacts = mutableMapOf<Report, MutableList<File>>()

    val testArtifacts
        @InputFiles
        get() = reportsToArtifacts.values.flatten()

    @InputFiles
    val classDirs = mutableListOf<FileCollection>()

    /* Task outputs */
    val reportFiles
        @OutputFiles
        get() = reportsToArtifacts.keys.map { it.reportFile }


    init {
        group = "Teamscale"
        description = "Generates a testwise coverage report"
    }

    private val jaCoCoTestwiseReportGenerator: JaCoCoTestwiseReportGenerator by lazy {
        JaCoCoTestwiseReportGenerator(
            classDirs.flatMap { it.files },
            agentFilter.getPredicate(),
            true,
            project.logger.wrapInILogger()
        )
    }
    lateinit var uploadTask: TeamscaleUploadTask

    /**
     * Generates a testwise coverage from the execution data and merges it with eventually existing closure coverage.
     */
    @TaskAction
    fun generateTestwiseCoverageReport() {
        logger.info("Generating coverage reports...")
        for ((reportConfig, artifacts) in reportsToArtifacts.entries) {
            val testDetails =
                ReportUtils.readObjects(ETestArtifactFormat.TEST_LIST, Array<TestDetails>::class.java, artifacts)
            val testExecutions = ReportUtils.readObjects(
                ETestArtifactFormat.TEST_EXECUTION,
                Array<TestExecution>::class.java,
                artifacts
            )

            val testwiseCoverage = getJaCoCoTestwiseCoverage(artifacts) ?: continue
            testwiseCoverage.add(getClosureTestwiseCoverage())

            logger.info("Merging report with ${testDetails.size} Details/${testwiseCoverage.tests.size} Coverage/${testExecutions.size} Results")

            val report = TestwiseCoverageReportBuilder.createFrom(testDetails, testwiseCoverage.tests, testExecutions)
            logger.info("Writing report to ${reportConfig.reportFile}")
            ReportUtils.writeReportToFile(reportConfig.reportFile, report)

            if (reportConfig.upload) {
                uploadTask.reports.add(reportConfig)
            }
        }
    }

    private fun getJaCoCoTestwiseCoverage(
        artifacts: List<File>
    ): TestwiseCoverage? {
        val jacocoExecutionData = ReportUtils.listFiles(ETestArtifactFormat.JACOCO, artifacts)
        if (jacocoExecutionData.isEmpty()) {
            logger.error("No execution data provided!")
            return null
        }
        logger.info("Generating testwise coverage for $jacocoExecutionData")

        return jaCoCoTestwiseReportGenerator.convert(jacocoExecutionData)
    }

    private fun getClosureTestwiseCoverage(): TestwiseCoverage? {
        val jsCoverageData = closureCoverageDir
        if (jsCoverageData.isEmpty()) {
            return null
        }
        return ClosureTestwiseCoverageGenerator(
            jsCoverageData,
            closureIncludeFilter.getPredicate()
        ).readTestCoverage()
    }

    fun addTestArtifactsDirs(report: Report, testArtifactDestination: File) {
        val list = reportsToArtifacts[report] ?: mutableListOf()
        list.add(testArtifactDestination)
        reportsToArtifacts[report] = list
    }
}

/** Wraps the gradle log4j logger into an ILogger. */
fun Logger.wrapInILogger(): ILogger {
    val logger = this
    return object : ILogger {
        override fun debug(message: String) = logger.debug(message)
        override fun info(message: String) = logger.info(message)
        override fun warn(message: String) = logger.warn(message)
        override fun warn(message: String, throwable: Throwable) = logger.warn(message, throwable)
        override fun error(throwable: Throwable) = logger.error("", throwable)
        override fun error(message: String, throwable: Throwable) = logger.error(message, throwable)
    }
}
