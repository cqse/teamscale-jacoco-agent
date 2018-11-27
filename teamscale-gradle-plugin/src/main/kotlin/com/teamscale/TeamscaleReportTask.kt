package com.teamscale

import com.google.gson.Gson
import com.teamscale.client.TestDetails
import com.teamscale.config.AgentConfiguration
import com.teamscale.config.GoogleClosureConfiguration
import com.teamscale.config.TeamscalePluginExtension
import com.teamscale.report.ReportUtils
import com.teamscale.report.testwise.ETestArtifactFormat
import com.teamscale.report.testwise.closure.ClosureTestwiseCoverageGenerator
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder
import com.teamscale.report.util.ILogger
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import java.io.File

/** Task which runs the impacted tests. */
open class TeamscaleReportTask : DefaultTask() {

    /**
     * Reference to the test task for which this task acts as a
     * stand-in when executing impacted tests.
     */
    lateinit var testTask: Test

    private val gson = Gson()

    /**
     * Reference to the configuration that should be used for this task.
     */
    lateinit var configuration: TeamscalePluginExtension

    /* Task inputs */

    private val agentConfiguration: AgentConfiguration
        @Input
        get() = configuration.agent

    private val closureIncludeFilter: GoogleClosureConfiguration.FileNameFilter
        @Input
        get() = configuration.report.googleClosureCoverage.getFilter()

    private val closureCoverageDir: Set<File>
        @InputFiles
        get() = configuration.report.googleClosureCoverage.destination ?: emptySet()

    private val testArtifactsDir
        @InputDirectory
        get() = configuration.agent.getTestArtifactDestination(project, testTask)

    /* Task outputs */

    private val reportFile
        @OutputFile
        get() = configuration.report.testwiseCoverage.getDestinationOrDefault(project, testTask)


    init {
        group = "Teamscale"
        description = "Generates a testwise coverage report"
    }

    /**
     * Generates a testwise coverage from the execution data and merges it with eventually existing closure coverage.
     */
    @TaskAction
    fun generateTestwiseCoverageReport() {
        logger.info("Generating coverage report...")

        val testDetails =
            ReportUtils.readObjects(ETestArtifactFormat.TEST_LIST, Array<TestDetails>::class.java, testArtifactsDir)
        val testExecutions = ReportUtils.readObjects(
            ETestArtifactFormat.TEST_EXECUTION,
            Array<TestExecution>::class.java,
            testArtifactsDir
        )

        val testwiseCoverage = getJaCoCoTestwiseCoverage() ?: return
        testwiseCoverage.add(getClosureTestwiseCoverage())

        logger.info("Merging report with ${testDetails.size} Details/${testwiseCoverage.tests.size} Coverage/${testExecutions.size} Results")

        val report = TestwiseCoverageReportBuilder.createFrom(testDetails, testwiseCoverage.tests, testExecutions)
        ReportUtils.writeReportToFile(reportFile, report)
    }

    private fun getJaCoCoTestwiseCoverage(): TestwiseCoverage? {
        val classDirectories = if (agentConfiguration.dumpClasses == true) {
            project.files(agentConfiguration.getDumpDirectory(project))
        } else {
            testTask.classpath
        }

        if (!testArtifactsDir.exists()) {
            logger.error("No execution data provided!")
            return null
        }
        val jacocoExecutionData = ReportUtils.listFiles(ETestArtifactFormat.JACOCO, testArtifactsDir)
        logger.info("Generating testwise coverage for $jacocoExecutionData")

        val generator = JaCoCoTestwiseReportGenerator(
            classDirectories.files,
            agentConfiguration.getFilter(),
            true,
            project.logger.wrapInILogger()
        )
        return generator.convert(jacocoExecutionData)
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
