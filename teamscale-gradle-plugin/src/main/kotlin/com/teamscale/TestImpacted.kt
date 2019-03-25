package com.teamscale

import com.teamscale.config.TeamscaleTaskExtension
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import java.io.File

/** Task which runs the impacted tests. */
@CacheableTask
open class TestImpacted : Test() {

    companion object {
        const val IMPACTED_TEST_ENGINE = "teamscale-test-impacted"
    }

    /** Command line switch to activate running all tests. */
    @Input
    @Option(
        option = "impacted",
        description = "If set Testwise coverage is recorded. By default only impacted tests are executed."
    )
    var runImpacted: Boolean = false

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

    private val reportConfiguration
        @Input
        get() = taskExtension.report

    private val agentFilterConfiguration
        @Input
        get() = taskExtension.agent.getFilter()

    private val agentJvmConfiguration
        @Input
        get() = taskExtension.agent.getAllAgents().map { it.getJvmArgs() }

    private val serverConfiguration
        @Input
        get() = taskExtension.parent.server

    /**
     * The (current) commit at which test details should be uploaded to.
     * Furthermore all changes up to including this commit are considered for test impact analysis.
     */
    private val endCommit
        @Internal
        get() = taskExtension.parent.commit.getOrResolveCommitDescriptor(project)


    /** The baseline. Only changes after the baseline are considered for determining the impacted tests. */
    private val baseline
        @Input
        @Optional
        get() = taskExtension.parent.baseline

    /** The directory to write the jacoco execution data to. */
    @Internal
    private lateinit var tempDir: File

    /** The report task used to setup and cleanup report directories. */
    @Internal
    lateinit var reportTask: TeamscaleReportTask

    @Internal
    private var includeEngines: Set<String> = emptySet()

    @Internal
    private val junitPlatformOptions: JUnitPlatformOptions = JUnitPlatformOptions()

    init {
        group = "Teamscale"
        description = "Executes the impacted tests and collects coverage per test case"
    }

    /** Overrides default behavior to only execute impacted tests. */
    override fun useJUnitPlatform(testFrameworkConfigure: Action<in JUnitPlatformOptions>) {
        testFrameworkConfigure.execute(junitPlatformOptions)

        if (junitPlatformOptions.excludeEngines.contains(IMPACTED_TEST_ENGINE)) {
            throw GradleException("Engine '$IMPACTED_TEST_ENGINE' can't be excluded in '$TestImpacted' Gradle task")
        }

        includeEngines = junitPlatformOptions.includeEngines

        super.useJUnitPlatform {
            it.excludeEngines = junitPlatformOptions.excludeEngines
            it.includeEngines = setOf("teamscale-test-impacted")
            it.includeTags = junitPlatformOptions.includeTags
            it.excludeTags = junitPlatformOptions.excludeTags
        }
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
        classpath = classpath.plus(project.configurations.getByName(TeamscalePlugin.impactedTestEngineConfiguration))

        jvmArgumentProviders.removeIf { it.javaClass.name.contains("JacocoPluginExtension") }

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

        reportTask.addTestArtifactsDirs(report, tempDir)
        reportConfig.googleClosureCoverage.destination?.let {
            reportTask.addTestArtifactsDirs(report, it)
        }
        reportTask.classDirs.add(classpath)

        setImpactedTestEngineOptions(report)
        super.executeTests()
    }

    private fun writeEngineProperty(name: String, value: String?) {
        if (value != null) {
            systemProperties["teamscale.test.impacted.$name"] = value
        }
    }

    private fun setImpactedTestEngineOptions(report: Report) {
        serverConfiguration.validate()
        writeEngineProperty("server.url", serverConfiguration.url!!)
        writeEngineProperty("server.project", serverConfiguration.project!!)
        writeEngineProperty("server.userName", serverConfiguration.userName!!)
        writeEngineProperty("server.userAccessToken", serverConfiguration.userAccessToken!!)
        writeEngineProperty("partition", report.partition)
        writeEngineProperty("endCommit", endCommit.toString())
        writeEngineProperty("baseline", baseline?.toString())
        writeEngineProperty("reportDirectory", tempDir.absolutePath)
        writeEngineProperty("agentsUrls", taskExtension.agent.getAllAgents().map { it.url }.joinToString(","))
        writeEngineProperty("runImpacted", runImpacted.toString())
        writeEngineProperty("runAllTests", runAllTests.toString())
        writeEngineProperty("engines", includeEngines.joinToString(","))
    }
}
