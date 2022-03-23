package com.teamscale

import com.teamscale.config.extension.TeamscalePluginExtension
import com.teamscale.config.extension.TeamscaleTestImpactedTaskExtension
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import javax.inject.Inject

/** Task which runs the impacted tests. */
@Suppress("MemberVisibilityCanBePrivate")
@CacheableTask
open class TestImpacted @Inject constructor(objects: ObjectFactory) : Test() {

    companion object {
        const val IMPACTED_TEST_ENGINE = "teamscale-test-impacted"
    }

    /** Command line switch to enable/disable testwise coverage collection. */
    @Input
    @Option(
        option = "collect-testwise-coverage",
        description = "If set Testwise coverage is recorded."
    )
    val collectTestwiseCoverage = objects.property(Boolean::class.java).convention(true)

    /** Command line switch to activate requesting from Teamscale which tests are impacted by a change (last commit be default). */
    @Input
    @Option(
        option = "impacted",
        description = "If set the plugin connects to Teamscale to retrieve impacted tests and an optimized order in " +
                "which they should be executed."
    )
    var runImpacted: Boolean = false

    /**
     * Command line switch to activate running all tests. This is the default if "--impacted" is false.
     * If "--impacted" is set this runs all test, but still requests on optimized order from Teamscale for the tests.
     */
    @Input
    @Option(
        option = "run-all-tests",
        description = "When set to true runs all tests even those that are not impacted. " +
                "Teamscale still tries to optimize the execution order to cause failures early."
    )
    var runAllTests: Boolean = false

    /** Command line switch to include or exclude added tests. */
    @Input
    @Option(
        option = "include-added-tests",
        description = "When set to true includes added tests in test selection."
    )
    var includeAddedTests: Boolean = true

    /** Command line switch to include or exclude failed and skipped tests. */
    @Input
    @Option(
        option = "include-failed-and-skipped",
        description = "When set to true includes failed and skipped tests in test selection."
    )
    var includeFailedAndSkipped: Boolean = true

    /**
     * Reference to the configuration that should be used for this task.
     */
    @Internal
    lateinit var pluginExtension: TeamscalePluginExtension

    /**
     * Reference to the configuration that should be used for this task.
     */
    @Internal
    lateinit var taskExtension: TeamscaleTestImpactedTaskExtension

    val reportConfiguration
        @Input
        get() = taskExtension.report.getReport()

    val agentFilterConfiguration
        @Input
        get() = taskExtension.agent.getFilter()

    val agentJvmConfiguration
        @Input
        get() = taskExtension.agent.getAllAgents().map { it.getJvmArgs() }

    val serverConfiguration
        @Input
        get() = pluginExtension.server

    /**
     * The (current) commit at which test details should be uploaded to.
     * Furthermore all changes up to including this commit are considered for test impact analysis.
     */
    val endCommit
        @Internal
        get() = pluginExtension.commit.getOrResolveCommitDescriptor(project).first


    /** The baseline. Only changes after the baseline are considered for determining the impacted tests. */
    val baseline
        @Input
        @Optional
        get() = pluginExtension.baseline

    /**
     * The directory to write the jacoco execution data to. Ensures that the directory
     * is cleared before executing the task by Gradle.
     */
    val reportOutputDir
        @OutputDirectory
        get() = taskExtension.agent.destination

    /** The report task used to setup and cleanup report directories. */
    @Internal
    lateinit var reportTask: TestwiseCoverageReportTask

    @Internal
    var includeEngines: Set<String> = emptySet()

    @Internal
    val junitPlatformOptions: JUnitPlatformOptions = JUnitPlatformOptions()

    val testEngineConfiguration: FileCollection
        @InputFiles
        @Classpath
        get() = project.configurations.getByName(TeamscalePlugin.impactedTestEngineConfiguration)

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
        if (collectTestwiseCoverage.get()) {
            classpath = classpath.plus(testEngineConfiguration)

            jvmArgumentProviders.removeIf { it.javaClass.name.contains("JacocoPluginExtension") }

            taskExtension.agent.localAgent?.let {
                jvmArgs(it.getJvmArgs())
            }

            val reportConfig = taskExtension.report
            val report = reportConfig.getReport()

            reportTask.addTestArtifactsDirs(report, reportOutputDir)

            getAllDependentJavaProjects(project).forEach { subProject ->
                val sourceSets = subProject.property("sourceSets") as SourceSetContainer
                reportTask.classDirs.addAll(sourceSets.map { it.output.classesDirs })
            }

            setImpactedTestEngineOptions(report)
        }
        super.executeTests()
    }

    private fun getAllDependentJavaProjects(project: Project): Set<Project> {
        return project.configurations
            .getByName("testRuntimeClasspath")
            .allDependencies
            .withType(ProjectDependency::class.java)
            .map { it.dependencyProject }
            .filter { it != project }
            .filter { it.pluginManager.hasPlugin("java") }
            .flatMap { getAllDependentJavaProjects(it) }
            .union(listOf(project))
    }

    private fun writeEngineProperty(name: String, value: String?) {
        if (value != null) {
            systemProperties["teamscale.test.impacted.$name"] = value
        }
    }

    private fun setImpactedTestEngineOptions(report: Report) {
        if (runImpacted) {
            assert(endCommit != null) { "When executing only impacted tests a branchName and timestamp must be specified!" }
            serverConfiguration.validate()
            writeEngineProperty("server.url", serverConfiguration.url!!)
            writeEngineProperty("server.project", serverConfiguration.project!!)
            writeEngineProperty("server.userName", serverConfiguration.userName!!)
            writeEngineProperty("server.userAccessToken", serverConfiguration.userAccessToken!!)
        }
        writeEngineProperty("partition", report.partition)
        writeEngineProperty("endCommit", endCommit?.toString())
        writeEngineProperty("baseline", baseline?.toString())
        writeEngineProperty("reportDirectory", reportOutputDir.absolutePath)
        writeEngineProperty("agentsUrls", taskExtension.agent.getAllAgents().map { it.url }.joinToString(","))
        writeEngineProperty("runImpacted", runImpacted.toString())
        writeEngineProperty("runAllTests", runAllTests.toString())
        writeEngineProperty("includeAddedTests", includeAddedTests.toString())
        writeEngineProperty("includeFailedAndSkipped", includeFailedAndSkipped.toString())
        writeEngineProperty("engines", includeEngines.joinToString(","))
    }
}
