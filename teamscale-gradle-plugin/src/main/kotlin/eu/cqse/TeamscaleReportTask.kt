package eu.cqse

import com.google.gson.Gson
import eu.cqse.config.AgentConfiguration
import eu.cqse.config.GoogleClosureConfiguration
import eu.cqse.config.TeamscalePluginExtension
import eu.cqse.teamscale.client.EReportFormat
import eu.cqse.teamscale.report.testwise.closure.ClosureTestwiseCoverageGenerator
import eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportGenerator
import eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportUtils
import eu.cqse.teamscale.report.testwise.model.TestDetails
import eu.cqse.teamscale.report.testwise.model.TestExecution
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverage
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverageReport
import eu.cqse.teamscale.report.util.ILogger
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
        get() = configuration.report.testwiseCoverage.getTempDestination(project, testTask)

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

        val testDetails = readObjects(testArtifactsDir, EReportFormat.TEST_LIST, TestDetailsList::class.java)
        val testExecutions = readObjects(testArtifactsDir, EReportFormat.TEST_EXECUTION, TestExecutionList::class.java)

        val testwiseCoverage = getJaCoCoTestwiseCoverage() ?: return
        testwiseCoverage.add(getClosureTestwiseCoverage())

        logger.info("Merging report with ${testDetails.size} Details/${testwiseCoverage.tests.size} Coverage/${testExecutions.size} Results")

        val report = TestwiseCoverageReport.createFrom(testDetails, testwiseCoverage.tests, testExecutions)
        TestwiseXmlReportUtils.writeReportToFile(
            reportFile, report
        )
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
        val jacocoExecutionData = listFiles(testArtifactsDir, "jacoco", "exec")
        logger.info("Generating testwise coverage for $jacocoExecutionData")

        val generator = TestwiseXmlReportGenerator(
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


    /** Recursively lists all files in the given directory that match the specified extension. */
    private fun <T> readObjects(file: File, format: EReportFormat, `class`: Class<out Collection<T>>): Collection<T> {
        return listFiles(file, format.filePrefix, format.extension).flatMap {
            gson.fromJson(
                it.reader(),
                `class`
            )
        }
    }

    private fun listFiles(file: File, prefix: String, extension: String): Set<File> {
        return file.walkTopDown().filter {
            it.isFile && it.name.startsWith(prefix) && it.extension.equals(
                extension,
                ignoreCase = true
            )
        }.toSet()
    }

    private class TestDetailsList : ArrayList<TestDetails>()
    private class TestExecutionList : ArrayList<TestExecution>()
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
