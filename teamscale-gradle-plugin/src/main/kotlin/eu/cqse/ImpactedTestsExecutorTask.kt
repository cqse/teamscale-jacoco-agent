package eu.cqse

import eu.cqse.config.TeamscalePluginExtension
import eu.cqse.teamscale.client.CommitDescriptor
import eu.cqse.teamscale.report.testwise.closure.ClosureTestwiseCoverageGenerator
import eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportGenerator
import eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportUtils
import eu.cqse.teamscale.report.util.AntPatternUtils
import eu.cqse.teamscale.report.util.ILogger
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.util.RelativePathUtil
import java.io.File

/** Task which runs the impacted tests. */
open class ImpactedTestsExecutorTask : JavaExec() {

    /** Command line switch to activate running all tests. */
    @Input
    @Option(
        option = "run-all-tests",
        description = "When set to true runs all tests, but still collects testwise coverage. By default only impacted tests are executed."
    )
    var runAllTests: Boolean = false

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

    /**
     * The baseline commit. All changes after this commit are considered
     * for test impact analysis.
     */
    @Input
    lateinit var baselineCommit: CommitDescriptor

    /**
     * The (current) commit at which test details should be uploaded to.
     * Furthermore all changes up to including this commit are considered for test impact analysis.
     */
    @Input
    lateinit var endCommit: CommitDescriptor

    /** The file to write the jacoco execution data to. */
    private lateinit var executionData: File

    init {
        group = "Teamscale"
        description = "Executes the impacted tests and collects coverage per test case"
        main = "org.junit.platform.console.ImpactedTestsExecutor"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    override fun exec() {
        prepareClassPath()
        executionData = configuration.agent.getExecutionData(project, testTask)

        workingDir = testTask.workingDir

        jvmArgs(getAgentJvmArg())

        args(getImpactedTestExecutorProgramArguments())

        logger.debug("Starting impacted tests executor with jvm args $jvmArgs")
        logger.debug("Starting impacted tests executor with args $args")
        logger.debug("With workingDir $workingDir")

        super.exec()

        generateCoverageReport()
    }

    private fun prepareClassPath() {
        if (classpath.files.isEmpty()) {
            classpath = testTask.classpath
        }

        classpath = classpath.plus(project.configurations.getByName(TeamscalePlugin.impactedTestExecutorConfiguration))
        logger.debug("Starting impacted tests with classpath ${classpath.files}")
    }

    /** Builds the jvm argument to start the impacted test executor. */
    private fun getAgentJvmArg(): String {
        val builder = StringBuilder()
        val argument = ArgumentAppender(builder, workingDir)
        builder.append("-javaagent:")
        val agentJar = project.configurations.getByName(TeamscalePlugin.impactedTestExecutorConfiguration)
            .filter { it.name.startsWith("impacted-tests-executor") }.first()
        builder.append(RelativePathUtil.relativePath(workingDir, agentJar))
        builder.append("=")
        argument.append("destfile", executionData)
        argument.append("includes", configuration.agent.includes)
        argument.append("excludes", configuration.agent.excludes)

        if (configuration.agent.dumpClasses == true) {
            argument.append("classdumpdir", configuration.agent.getDumpDirectory(project))
        }

        return builder.toString()
    }

    /**
     * Generates a testwise coverage from the execution data and merges it with eventually existing closure coverage.
     */
    private fun generateCoverageReport() {
        logger.info("Generating coverage report...")
        val classDirectories = if (configuration.agent.dumpClasses == true) {
            project.files(configuration.agent.getDumpDirectory(project))
        } else {
            classpath
        }

        if (!executionData.exists()) {
            logger.error("No execution data provided!")
            return
        }

        val generator = TestwiseXmlReportGenerator(
            classDirectories.files,
            configuration.agent.getFilter(),
            true,
            project.logger.wrapInILogger()
        )
        val testwiseCoverage = generator.convert(executionData)
        val jsCoverageData = configuration.report.googleClosureCoverage.destination ?: emptySet()
        if (!jsCoverageData.isEmpty()) {
            val closureTestwiseCoverage = ClosureTestwiseCoverageGenerator(
                jsCoverageData,
                configuration.report.googleClosureCoverage.getFilter()
            ).readTestCoverage()
            testwiseCoverage.merge(closureTestwiseCoverage)
        }
        TestwiseXmlReportUtils.writeReportToFile(
            configuration.report.testwiseCoverage.getDestinationOrDefault(project, testTask), testwiseCoverage
        )
    }

    private fun getImpactedTestExecutorProgramArguments(): List<String> {
        val args = mutableListOf(
            "--url", configuration.server.url!!,
            "--project", configuration.server.project!!,
            "--user", configuration.server.userName!!,
            "--access-token", configuration.server.userAccessToken!!,
            "--partition", configuration.report.testwiseCoverage.getTransformedPartition(project),
            "--baseline", baselineCommit.toString(),
            "--end", endCommit.toString()
        )

        if (runAllTests) {
            args.add("--all")
        }

        addFilters(args)

        if (configuration.report.jUnit.upload != null) {
            args.add("--reports-dir")
            args.add(configuration.report.jUnit.getDestinationOrDefault(project, testTask).absolutePath)
        }

        val rootDirs = mutableListOf<File>()
        project.sourceSets.forEach { sourceSet ->
            val output = sourceSet.output
            rootDirs.addAll(output.classesDirs.files)
            rootDirs.add(output.resourcesDir)
            rootDirs.addAll(output.dirs.files)
        }
        args.addAll(listOf("--scan-class-path", rootDirs.joinToString(File.pathSeparator)))

        return args
    }

    private fun addFilters(args: MutableList<String>) {
        val platformOptions = (testTask.testFramework as JUnitPlatformTestFramework).options
        platformOptions.includeTags.forEach { tag ->
            args.addAll(listOf("-t", tag))
        }
        platformOptions.excludeTags.forEach { tag ->
            args.addAll(listOf("-T", tag))
        }
        platformOptions.includeEngines.forEach { engineId ->
            args.addAll(listOf("-e", engineId))
        }
        platformOptions.excludeEngines.forEach { engineId ->
            args.addAll(listOf("-E", engineId))
        }
        val includes = testTask.includes
        // JUnit by default only includes classes ending with Test, but we want to include all classes.
        if (includes.isEmpty()) {
            args.addAll(listOf("-n", ".*"))
        }
        includes.forEach { classIncludePattern ->
            args.addAll(listOf("-n", AntPatternUtils.convertPattern(classIncludePattern, false).pattern()))
        }
        testTask.excludes.forEach { classExcludePattern ->
            args.addAll(listOf("-N", AntPatternUtils.convertPattern(classExcludePattern, false).pattern()))
        }
    }
}

/** Wraps the gradle log4j logger into an ILogger. */
fun Logger.wrapInILogger(): ILogger {
    val logger = this
    return object : ILogger {
        override fun debug(message: String) = logger.debug(message)
        override fun warn(message: String) = logger.warn(message)
        override fun warn(message: String, throwable: Throwable) = logger.warn(message, throwable)
        override fun error(throwable: Throwable) = logger.error("", throwable)
        override fun error(message: String, throwable: Throwable) = logger.error(message, throwable)
    }
}

/** Returns the sourceSets container of the given project. */
val Project.sourceSets: SourceSetContainer
    get() {
        return convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    }
