package com.teamscale

import com.teamscale.client.TestDetails
import com.teamscale.config.GoogleClosureConfiguration
import com.teamscale.config.SerializableFilter
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import java.io.File

/** Task which runs the impacted tests. */
open class TeamscaleReportTask : DefaultTask() {

    /**
     * Test task's name for which this task acts as a
     * stand-in when executing impacted tests.
     */
    lateinit var testTaskName: String

    /**
     * Reference to the configuration that should be used for this task.
     */
    lateinit var configuration: TeamscalePluginExtension

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

    @InputFiles
    val testArtifactsDirs = mutableListOf<File>()

    @InputFiles
    val classDirs = mutableListOf<File>()

    /* Task outputs */

    private val reportFile
        @OutputFile
        get() = configuration.report.testwiseCoverage.getDestinationOrDefault(project, testTaskName)


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
            ReportUtils.readObjects(ETestArtifactFormat.TEST_LIST, Array<TestDetails>::class.java, testArtifactsDirs)
        val testExecutions = ReportUtils.readObjects(
            ETestArtifactFormat.TEST_EXECUTION,
            Array<TestExecution>::class.java,
            testArtifactsDirs
        )

        val testwiseCoverage = getJaCoCoTestwiseCoverage() ?: return
        testwiseCoverage.add(getClosureTestwiseCoverage())

        logger.info("Merging report with ${testDetails.size} Details/${testwiseCoverage.tests.size} Coverage/${testExecutions.size} Results")

        val report = TestwiseCoverageReportBuilder.createFrom(testDetails, testwiseCoverage.tests, testExecutions)
        ReportUtils.writeReportToFile(reportFile, report)
    }

    private fun getJaCoCoTestwiseCoverage(): TestwiseCoverage? {
        val jacocoExecutionData = ReportUtils.listFiles(ETestArtifactFormat.JACOCO, testArtifactsDirs)
        if (jacocoExecutionData.isEmpty()) {
            logger.error("No execution data provided!")
            return null
        }
        logger.info("Generating testwise coverage for $jacocoExecutionData")

        val generator = JaCoCoTestwiseReportGenerator(
            classDirs,
            agentFilter.getPredicate(),
            true,
            project.logger.wrapInILogger()
        )
        return generator.convert(jacocoExecutionData)
    }

    fun addTestCoverage(config: TeamscalePluginExtension, testTask: Test) {
        val classDirectories = if (config.agent.dumpClasses == true) {
            project.files(config.agent.getDumpDirectory(project))
        } else {
            testTask.classpath
        }
        classDirs.addAll(classDirectories.files)
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
