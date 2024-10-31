package com.teamscale

import com.teamscale.config.extension.TeamscalePluginExtension
import com.teamscale.config.extension.TeamscaleTestImpactedTaskExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject

/** Task which runs the impacted tests. */
@Suppress("MemberVisibilityCanBePrivate")
@CacheableTask
abstract class TestImpacted @Inject constructor(objects: ObjectFactory) : Test() {

	companion object {
		const val IMPACTED_TEST_ENGINE = "teamscale-test-impacted"
	}

	/** Command line switch to enable/disable testwise coverage collection. */
	@Input
	val collectTestwiseCoverage: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

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
		@Nested
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

	/**
	 *  Can be used instead of [endCommit] by using a revision (e.g. git SHA1) instead of a branch and timestamp.
	 */
	val endRevision
		@Internal
		get() = pluginExtension.commit.getOrResolveCommitDescriptor(project).second

	/** The baseline. Only changes after the baseline are considered for determining the impacted tests. */
	val baseline
		@Input
		@Optional
		get() = pluginExtension.baseline

	/**
	 * Can be used instead of [baseline] by using a revision (e.g. git SHA1) instead of a branch and timestamp
	 */
	val baselineRevision
		@Input
		@Optional
		get() = pluginExtension.baselineRevision

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	val repository
		@Input
		@Optional
		get() = pluginExtension.repository

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

	val testEngineConfiguration: FileCollection
		@InputFiles
		@Classpath
		get() = project.configurations.getByName(TeamscalePlugin.impactedTestEngineConfiguration)

	init {
		group = "Teamscale"
		description = "Executes the impacted tests and collects coverage per test case"
	}

	@TaskAction
	override fun executeTests() {
		val testFrameworkOptions = options
		require(testFrameworkOptions is JUnitPlatformOptions) { "Only JUnit Platform is supported as test framework!" }
		if (collectTestwiseCoverage.get()) {
			require(maxParallelForks == 1) { "maxParallelForks is ${maxParallelForks}. Testwise coverage collection is only supported for maxParallelForks=1!" }

			classpath = classpath.plus(testEngineConfiguration)

			if (runImpacted) {
				// Workaround to not cause the task to fail when no tests are executed, which might happen when no tests are impacted
				// We do so by adding a useless filter, because the "no tests executed" can only be disabled for the case where filters are applied
				// https://docs.gradle.org/8.8/userguide/upgrading_version_8.html#test_task_fail_on_no_test_executed
				// https://github.com/gradle/gradle/blob/57cc16c8fbf4116a0ef0ad7b742c1a4a4e11a474/platforms/software/testing-base/src/main/java/org/gradle/api/tasks/testing/AbstractTestTask.java#L542
				filter.excludeTestsMatching("dummy-test-name")
				filter.isFailOnNoMatchingTests = false
			}

			jvmArgumentProviders.removeIf { it.javaClass.name.contains("JacocoPluginExtension") }

			taskExtension.agent.localAgent?.let {
				jvmArgs(it.getJvmArgs())
			}

			val reportConfig = taskExtension.report
			val report = reportConfig.getReport().copy(partial = runImpacted && !runAllTests)

			reportTask.addTestArtifactsDirs(report, reportOutputDir)

			collectAllDependentJavaProjects(project).forEach { subProject ->
				val sourceSets = subProject.property("sourceSets") as SourceSetContainer
				reportTask.classDirs.addAll(sourceSets.map { it.output.classesDirs })
			}

			setImpactedTestEngineOptions(report, testFrameworkOptions)
			testFrameworkOptions.includeEngines = setOf(IMPACTED_TEST_ENGINE)
		}
		super.executeTests()
	}

	private fun collectAllDependentJavaProjects(
		project: Project,
		seenProjects: MutableSet<Project> = mutableSetOf()
	): Set<Project> {
		// seenProjects helps to detect cycles in the dependency graph
		if (seenProjects.contains(project) || !project.pluginManager.hasPlugin("java")) {
			return setOf()
		}
		seenProjects.add(project)
		return project.configurations
			.getByName("testRuntimeClasspath")
			.allDependencies
			.withType<ProjectDependency>()
			.map { it.dependencyProject }
			.flatMap { collectAllDependentJavaProjects(it, seenProjects) }
			.union(listOf(project))
	}

	private fun writeEngineProperty(name: String, value: String?) {
		if (value != null) {
			systemProperties["teamscale.test.impacted.$name"] = value
		}
	}

	private fun setImpactedTestEngineOptions(report: Report, options: JUnitPlatformOptions) {
		if (runImpacted) {
			assert(endCommit != null) { "When executing only impacted tests a branchName and timestamp must be specified!" }
			serverConfiguration.validate()
			writeEngineProperty("server.url", serverConfiguration.url)
			writeEngineProperty("server.project", serverConfiguration.project)
			writeEngineProperty("server.userName", serverConfiguration.userName)
			writeEngineProperty("server.userAccessToken", serverConfiguration.userAccessToken)
		}
		writeEngineProperty("partition", report.partition.get())
		writeEngineProperty("endCommit", endCommit?.toString())
		writeEngineProperty("endRevision", endRevision)
		writeEngineProperty("baseline", baseline?.toString())
		writeEngineProperty("baselineRevision", baselineRevision)
		writeEngineProperty("repository", repository)
		writeEngineProperty("reportDirectory", reportOutputDir.absolutePath)
		writeEngineProperty("agentsUrls", taskExtension.agent.getAllAgents().map { it.url }.joinToString(","))
		writeEngineProperty("runImpacted", runImpacted.toString())
		writeEngineProperty("runAllTests", runAllTests.toString())
		writeEngineProperty("includeAddedTests", includeAddedTests.toString())
		writeEngineProperty("includeFailedAndSkipped", includeFailedAndSkipped.toString())
		writeEngineProperty("includedEngines", options.includeEngines.joinToString(","))
		writeEngineProperty("excludedEngines", options.excludeEngines.joinToString(","))
	}
}
