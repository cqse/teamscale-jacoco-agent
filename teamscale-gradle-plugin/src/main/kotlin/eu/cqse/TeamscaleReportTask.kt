package eu.cqse

import com.google.gson.Gson
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import java.io.File

/** Task which runs the impacted tests. */
open class TeamscaleReportTask : DefaultTask() {

    /**
     * Reference to the test task for which this task acts as a
     * stand-in when executing impacted tests.
     */
    lateinit var testTask: Test

    /**
     * Reference to the configuration that should be used for this task.
     */
    @Input
    lateinit var configuration: TeamscalePluginExtension

    init {
        group = "Teamscale"
        description = "Generates a testwise coverage report"
    }

    private val gson = Gson()

    /**
     * Generates a testwise coverage from the execution data and merges it with eventually existing closure coverage.
     */
    @TaskAction
    fun generateTestwiseCoverageReport() {
        logger.info("Generating coverage report...")

        val tempDestination = configuration.report.testwiseCoverage.getTempDestination(project, testTask)
        val testDetails = readObjects(tempDestination, EReportFormat.TEST_LIST, TestDetailsList::class.java)
        val testExecutions = readObjects(tempDestination, EReportFormat.TEST_EXECUTION, TestExecutionList::class.java)

        val testwiseCoverage = getJaCoCoTestwiseCoverage() ?: return
        testwiseCoverage.add(getClosureTestwiseCoverage())

        val report = TestwiseCoverageReport.createFrom(testDetails, testwiseCoverage.tests, testExecutions)
        TestwiseXmlReportUtils.writeReportToFile(
            configuration.report.testwiseCoverage.getDestinationOrDefault(project, testTask), report
        )
    }

    private fun getJaCoCoTestwiseCoverage(): TestwiseCoverage? {
        val executionData = configuration.agent.getExecutionData(project, testTask)

        val classDirectories = if (configuration.agent.dumpClasses == true) {
            project.files(configuration.agent.getDumpDirectory(project))
        } else {
            testTask.classpath
        }

        if (!executionData.exists()) {
            logger.error("No execution data provided!")
            return null
        }

        val generator = TestwiseXmlReportGenerator(
            classDirectories.files,
            configuration.agent.getFilter(),
            true,
            project.logger.wrapInILogger()
        )
        return generator.convert(executionData)
    }

    private fun getClosureTestwiseCoverage(): TestwiseCoverage? {
        val jsCoverageData = configuration.report.googleClosureCoverage.destination ?: emptySet()
        if (jsCoverageData.isEmpty()) {
            return null
        }
        return ClosureTestwiseCoverageGenerator(
            jsCoverageData,
            configuration.report.googleClosureCoverage.getFilter()
        ).readTestCoverage()
    }


    /** Recursively lists all files in the given directory that match the specified extension. */
    private fun <T> readObjects(file: File, format: EReportFormat, `class`: Class<out Collection<T>>): Collection<T> {
        return file.walkTopDown().filter {
            it.isFile && it.name.startsWith(format.filePrefix) && it.extension.equals(
                format.extension,
                ignoreCase = true
            )
        }.toSet().flatMap {
            gson.fromJson(
                it.reader(),
                `class`
            )
        }
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
