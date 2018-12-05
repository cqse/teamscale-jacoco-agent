package com.teamscale

import com.teamscale.client.CommitDescriptor
import com.teamscale.config.TeamscalePluginExtension
import com.teamscale.report.util.AntPatternUtils
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import java.io.File
import java.util.regex.Pattern

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
     * The (current) commit at which test details should be uploaded to.
     * Furthermore all changes up to including this commit are considered for test impact analysis.
     */
    @Input
    lateinit var endCommit: CommitDescriptor

    /** The directory to write the jacoco execution data to. */
    private lateinit var tempDir: File

    init {
        group = "Teamscale"
        description = "Executes the impacted tests and collects coverage per test case"
        main = "org.junit.platform.console.ImpactedTestsExecutor"
    }

    @TaskAction
    override fun exec() {
        prepareClassPath()
        tempDir = configuration.agent.getTestArtifactDestination(project, testTask.name)

        if (tempDir.exists()) {
            logger.debug("Removing old execution data file at ${tempDir.absolutePath}")
            tempDir.deleteRecursively()
        }

        workingDir = testTask.workingDir

        configuration.agent.localAgent?.let {
            jvmArgs(it.getJvmArgs(project, testTask.name))
        }

        args(getImpactedTestExecutorProgramArguments())

        logger.info("Starting agent with jvm args $jvmArgs")
        logger.info("Starting impacted tests executor with args $args")
        logger.info("With workingDir $workingDir")

        super.exec()
    }

    private fun prepareClassPath() {
        if (classpath.files.isEmpty()) {
            classpath = testTask.classpath
        }

        classpath = classpath.plus(project.configurations.getByName(TeamscalePlugin.impactedTestExecutorConfiguration))
        logger.debug("Starting impacted tests with classpath ${classpath.files}")
    }

    private fun getImpactedTestExecutorProgramArguments(): List<String> {
        val args = mutableListOf(
            "--url", configuration.server.url!!,
            "--project", configuration.server.project!!,
            "--user", configuration.server.userName!!,
            "--access-token", configuration.server.userAccessToken!!,
            "--partition", configuration.report.testwiseCoverage.getTransformedPartition(project),
            "--end", endCommit.toString()
        )

        configuration.agent.getAllAgents().forEach {
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
        testTask.excludes.forEach { classExcludePattern ->
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
