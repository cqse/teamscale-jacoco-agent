package eu.cqse

import eu.cqse.config.TeamscalePluginExtension
import eu.cqse.teamscale.client.CommitDescriptor
import eu.cqse.teamscale.report.testwise.closure.ClosureTestwiseCoverageGenerator
import eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportGenerator
import eu.cqse.teamscale.report.testwise.jacoco.TestwiseXmlReportUtils
import eu.cqse.teamscale.report.util.ILogger
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.util.RelativePathUtil
import java.io.File

open class ImpactedTestsExecutor : JavaExec() {

    @Input
    @Option(option = "run-all-tests", description = "When set to true runs all tests, but still collects testwise coverage. By default only impacted tests are executed.")
    var runAllTests: Boolean = false

    lateinit var testTask: Test

    @Input
    lateinit var configuration: TeamscalePluginExtension
    @Input
    lateinit var commitDescriptor: CommitDescriptor

    private lateinit var executionData: File

    @Input
    private lateinit var platformOptions: JUnitPlatformOptions

    init {
        group = "Teamscale"
        description = "Executes the impacted tests and collects coverage per test case"
        main = "org.junit.platform.console.CustomConsoleLauncher"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    override fun exec() {
        prepareClassPath()
        executionData = configuration.agent.getExecutionData(project, testTask)

        workingDir = testTask.workingDir

        jvmArgs(getAgentJvmArg())

        args(getCoverageConductorArgs())

        logger.debug("Starting coverage conductor with jvm args " + jvmArgs.toString())
        logger.debug("Starting coverage conductor with args " + args.toString())
        logger.debug("With workingDir " + workingDir.toString())

        super.exec()

        generateCoverageReport()
    }

    private fun prepareClassPath() {
        if (classpath.files.isEmpty()) {
            classpath = testTask.classpath
        }

        classpath = classpath.plus(project.configurations.getByName("coverageConductor"))
        logger.debug("Starting impacted tests with classpath " + classpath.files.toString())
    }

    private fun getAgentJvmArg(): String {
        val builder = StringBuilder()
        val argument = ArgumentAppender(builder, workingDir)
        builder.append("-javaagent:")
        val agentJar = project.configurations.getByName("coverageConductor")
                .filter { it.name.startsWith("coverage-conductor") }.first()
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

        val generator = TestwiseXmlReportGenerator(classDirectories.files, configuration.agent.getFilter(), true, createLogger())
        val testwiseCoverage = generator.convert(executionData)
        val jsCoverageData = configuration.report.googleClosureCoverage.destination ?: emptySet()
        if (!jsCoverageData.isEmpty()) {
            val closureTestwiseCoverage = ClosureTestwiseCoverageGenerator(jsCoverageData,
                    configuration.report.googleClosureCoverage.getFilter()).readTestCoverage()
            testwiseCoverage.merge(closureTestwiseCoverage)
        }
        TestwiseXmlReportUtils.writeReportToFile(
                configuration.report.testwiseCoverage.getDestinationOrDefault(project, testTask), testwiseCoverage)
    }

    fun configure(project: Project) {
        this.dependsOn.add(getSourceSets(project).getByName("test").runtimeClasspath)
        this.dependsOn.add(project.configurations.getByName("coverageConductor"))
        platformOptions = (testTask.testFramework as JUnitPlatformTestFramework).options
    }

    private fun getCoverageConductorArgs(): List<String> {
        val args = mutableListOf(
                "--url", configuration.server.url!!,
                "--project", configuration.server.project!!,
                "--user", configuration.server.userName!!,
                "--access-token", configuration.server.userAccessToken!!,
                "--partition", configuration.report.testwiseCoverage.getTransformedPartition(project),
                "--end", commitDescriptor.toString())

        if (runAllTests) {
            args.add("--all")
        }

        addFilters(platformOptions, args)

        //TODO make optional
        args.add("--reports-dir")
        args.add(configuration.report.jUnit.getDestinationOrDefault(project, testTask).absolutePath)

        val rootDirs = mutableListOf<File>()
        getSourceSets(project).forEach { sourceSet ->
            val output = sourceSet.output
            rootDirs.addAll(output.classesDirs.files)
            rootDirs.add(output.resourcesDir)
            rootDirs.addAll(output.dirs.files)
        }
        args.addAll(listOf("--scan-class-path", rootDirs.joinToString(File.pathSeparator)))

        return args
    }

    private fun addFilters(platformOptions: JUnitPlatformOptions, args: MutableList<String>) {
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
        configuration.includes.forEach { classIncludePattern ->
            args.addAll(listOf("-n", classIncludePattern))
        }
        configuration.excludes.forEach { classExcludePattern ->
            args.addAll(listOf("-N", classExcludePattern))
        }
    }

    private fun getSourceSets(project: Project): SourceSetContainer {
        return project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    }

    private fun createLogger(): ILogger {
        return object : ILogger {
            override fun debug(debugLog: String) {
                logger.debug(debugLog)
            }

            override fun warn(message: String) {
                logger.warn(message)
            }

            override fun warn(s: String, e: Throwable) {
                logger.warn(s, e)
            }

            override fun error(e: Throwable) {
                logger.error("", e)
            }

            override fun error(message: String, throwable: Throwable) {
                logger.error(message, throwable)
            }

        }
    }

    private class ArgumentAppender(private val builder: StringBuilder, private val workingDirectory: File) {
        private var anyArgs: Boolean = false

        fun append(name: String, value: Any?) {
            if (value == null) {
                return
            }

            if (value is Collection<*>) {
                if (!value.isEmpty()) {
                    appendKeyValue(name, value.joinToString(":"))
                }
            } else if (value is File) {
                appendKeyValue(name, RelativePathUtil.relativePath(workingDirectory, value))
            } else {
                appendKeyValue(name, value.toString())
            }

        }

        private fun appendKeyValue(key: String, value: String) {
            if (anyArgs) {
                builder.append(",")
            }

            builder.append(key).append("=").append(value)
            anyArgs = true
        }
    }
}
