package com.teamscale

import com.teamscale.config.TeamscaleTaskExtension
import com.teamscale.report.util.AntPatternUtils
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import org.gradle.process.internal.ExecActionFactory
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject

/** Task which runs the impacted tests. */
open class TestImpacted : Test() {

    /** Command line switch to activate running all tests. */
    @Input
    @Option(
        option = "impacted",
        description = "If set only impacted tests are executed."
    )
    var onlyRunImpacted: Boolean = false

    /** Command line switch to activate running all tests. */
    @Input
    @Option(
        option = "run-all-tests",
        description = "When set to true runs all tests, but still collects testwise coverage."
    )
    var runAllTests: Boolean = false

    /**
     * Reference to the configuration that should be used for this task.
     */
    @Internal
    internal lateinit var taskExtension: TeamscaleTaskExtension

    @get:Input
    val reportConfiguration
        get() = taskExtension.report

    @get:Input
    val agentFilterConfiguration
        get() = taskExtension.agent.getFilter()

    @get:Input
    val agentJvmConfiguration
        get() = taskExtension.agent.getAllAgents().map { it.getJvmArgs() }

    @get:Input
    val serverConfiguration
        get() = taskExtension.parent.server

    /**
     * The (current) commit at which test details should be uploaded to.
     * Furthermore all changes up to including this commit are considered for test impact analysis.
     */
    val endCommit
        @Input
        get() =  taskExtension.parent.commit.getCommitDescriptor()

    /** The directory to write the jacoco execution data to. */
    @Internal
    private lateinit var tempDir: File

    @Internal
    lateinit var reportTask: TeamscaleReportTask

    init {
        group = "Teamscale"
        description = "Executes the impacted tests and collects coverage per test case"
        useJUnitPlatform()
    }

    @Inject
    protected open fun getExecActionFactory(): ExecActionFactory {
        throw UnsupportedOperationException()
    }

    override fun useJUnit() {
        throw GradleException("JUnit 4 is not supported! Use JUnit Platform instead!")
    }

    override fun useJUnit(testFrameworkConfigure: Closure<*>?) {
        throw GradleException("JUnit 4 is not supported! Use JUnit Platform instead!")
    }

    override fun useJUnit(testFrameworkConfigure: Action<in JUnitOptions>) {
        throw GradleException("JUnit 4 is not supported! Use JUnit Platform instead!")
    }

    override fun useTestNG() {
        throw GradleException("TestNG is not supported! Use JUnit Platform instead!")
    }

    override fun useTestNG(testFrameworkConfigure: Closure<Any>) {
        throw GradleException("TestNG is not supported! Use JUnit Platform instead!")
    }

    override fun useTestNG(testFrameworkConfigure: Action<in TestNGOptions>) {
        throw GradleException("TestNG is not supported! Use JUnit Platform instead!")
    }

    @TaskAction
    override fun executeTests() {
        if (!onlyRunImpacted) {
            super.executeTests()
            return
        } else {
            runImpactedTests()
        }
    }

    private fun runImpactedTests() {
        prepareClassPath()

        tempDir = taskExtension.agent.destination
        if (tempDir.exists()) {
            logger.debug("Removing old execution data file(s) at ${tempDir.absolutePath}")
            tempDir.deleteRecursively()
        }

        taskExtension.agent.localAgent?.let {
            jvmArgs(it.getJvmArgs())
        }

        val reportConfig = taskExtension.getMergedReports()
        val report = reportConfig.testwiseCoverage.getReport(project, this)
        reportTask.addTestArtifactsDirs(report, taskExtension.agent.destination)
        reportTask.classDirs.add(classpath)

        val javaExecHandleBuilder = getExecActionFactory().newJavaExecAction()
        this.copyTo(javaExecHandleBuilder)

        javaExecHandleBuilder.main = "org.junit.platform.console.ImpactedTestsExecutor"
        javaExecHandleBuilder.args = getImpactedTestExecutorProgramArguments(report)

        logger.info("Starting agent with jvm args $jvmArgs")
        logger.info("Starting impacted tests executor with args $javaExecHandleBuilder.args")
        logger.info("With workingDir $workingDir")

        javaExecHandleBuilder.execute()
    }

    private fun prepareClassPath() {
        classpath = classpath.plus(project.configurations.getByName(TeamscalePlugin.impactedTestExecutorConfiguration))
        logger.debug("Starting impacted tests with classpath ${classpath.files}")
    }

    private fun getImpactedTestExecutorProgramArguments(report: Report): List<String> {
        val args = mutableListOf(
            "--url", serverConfiguration.url!!,
            "--project", serverConfiguration.project!!,
            "--user", serverConfiguration.userName!!,
            "--access-token", serverConfiguration.userAccessToken!!,
            "--partition", report.partition,
            "--end", endCommit.toString()
        )

        taskExtension.agent.getAllAgents().forEach {
            args.addAll(listOf("--agent", it.url.toString()))
        }

        if (runAllTests) {
            args.add("--all")
        }

        addFilters(args)

        args.add("--reports-dir")
        args.add(tempDir.absolutePath)

        val rootDirs = mutableListOf<File>()
        project.sourceSets.forEach { sourceSet ->
            val output = sourceSet.output
            rootDirs.addAll(output.classesDirs.files)
            if (output.resourcesDir != null) {
                rootDirs.add(output.resourcesDir!!)
            }
            rootDirs.addAll(output.dirs.files)
        }
        args.addAll(listOf("--scan-class-path", rootDirs.joinToString(File.pathSeparator)))

        return args
    }

    private fun addFilters(args: MutableList<String>) {
        val platformOptions = (testFramework as JUnitPlatformTestFramework).options
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
        // JUnit by default only includes classes ending with Test, but we want to include all classes.
        if (includes.isEmpty()) {
            // .* Needs to be set in quotes as windows expands this to all file names starting with dot otherwise
            args.addAll(listOf("-n", "\".*\""))
        }
        includes.forEach { classIncludePattern ->
            args.addAll(
                listOf(
                    "-n", '"' + normalizeAntPattern(
                        classIncludePattern
                    ).pattern() + '"'
                )
            )
        }
        excludes.forEach { classExcludePattern ->
            args.addAll(
                listOf(
                    "-N", '"' + normalizeAntPattern(
                        classExcludePattern
                    ).pattern() + '"'
                )
            )
        }
    }

    companion object {

        fun normalizeAntPattern(antPattern: String): Pattern =
            AntPatternUtils.convertPattern(normalize(antPattern), false)

        fun normalize(pattern: String): String {
            return pattern
                .replace("\\.class$".toRegex(), "")
                .replace("[/\\\\]".toRegex(), ".")
                .replace(".?\\*\\*.?".toRegex(), "**")
        }

    }
}

/** Returns the sourceSets container of the given project. */
val Project.sourceSets: SourceSetContainer
    get() {
        return convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    }
