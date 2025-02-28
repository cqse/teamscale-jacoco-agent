package com.teamscale

import com.teamscale.client.CommitDescriptor
import com.teamscale.config.AgentConfiguration
import com.teamscale.config.ServerConfiguration
import com.teamscale.internal.DefaultTestImpactedTaskReports
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestTaskReports
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.util.internal.ClosureBackedAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/** Task which runs the impacted tests. */
@Suppress("MemberVisibilityCanBePrivate")
@DisableCachingByDefault(because = "The task relies on Teamscale as an external system, so we cannot guarantee deterministic outputs")
abstract class TestImpacted @Inject constructor(objects: ObjectFactory) : Test() {

	companion object {
		const val IMPACTED_TEST_ENGINE = "teamscale-test-impacted"
	}

	/** Command line switch to activate requesting from Teamscale which tests are impacted by a change (last commit be default). */
	@get:Input
	@get:Option(
		option = "impacted",
		description = "If set the plugin connects to Teamscale to retrieve impacted tests and an optimized order in " +
				"which they should be executed."
	)
	abstract val runImpacted: Property<Boolean>

	/**
	 * Command line switch to activate running all tests. This is the default if "--impacted" is false.
	 * If "--impacted" is set this runs all test, but still requests on optimized order from Teamscale for the tests.
	 */
	@get:Input
	@get:Option(
		option = "run-all-tests",
		description = "When set to true runs all tests even those that are not impacted. " +
				"Teamscale still tries to optimize the execution order to cause failures early."
	)
	abstract val runAllTests: Property<Boolean>

	/** Command line switch to include or exclude added tests. */
	@get:Input
	@get:Option(
		option = "include-added-tests",
		description = "When set to true includes added tests in test selection."
	)
	abstract val includeAddedTests: Property<Boolean>

	/** Command line switch to include or exclude failed and skipped tests. */
	@get:Input
	@get:Option(
		option = "include-failed-and-skipped",
		description = "When set to true includes failed and skipped tests in test selection."
	)
	abstract val includeFailedAndSkipped: Property<Boolean>

	@get:Input
	abstract val partition: Property<String>

	@get:Input
	internal abstract val serverConfiguration: Property<ServerConfiguration>

	/**
	 * The directory to write the jacoco execution data to. Ensures that the directory
	 * is cleared before executing the task by Gradle.
	 */
	@get:Nested
	internal abstract val agentConfiguration: Property<AgentConfiguration>

	/**
	 * The commit (branch+timestamp or revision e.g. git SHA1) at which test details should be uploaded to.
	 * Furthermore, all changes up to including this commit are considered for test impact analysis.
	 */
	@get:Input
	@get:Optional
	internal abstract val endCommit: Property<Pair<CommitDescriptor?, String?>>

	/** The baseline. Only changes after the baseline are considered for determining the impacted tests. */
	@get:Input
	@get:Optional
	abstract val baseline: Property<Long>

	/**
	 * Can be used instead of [baseline] by using a revision (e.g. git SHA1) instead of a branch and timestamp
	 */
	@get:Input
	@get:Optional
	abstract val baselineRevision: Property<String>

	/**
	 * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
	 * Null or empty will lead to a lookup in all repositories in the Teamscale project.
	 */
	@get:Input
	@get:Optional
	abstract val repository: Property<String>

	@get:InputFiles
	@get:Classpath
	internal abstract val testEngineConfiguration: ConfigurableFileCollection

	private val impactedReports: TestImpactedTaskReports

	init {
		group = "Teamscale"
		description = "Executes the impacted tests and collects coverage per test case"
		impactedReports = DefaultTestImpactedTaskReports(super.getReports(), objects)
	}

	@TaskAction
	override fun executeTests() {
		val testFrameworkOptions = options
		check(testFrameworkOptions is JUnitPlatformOptions) { "Only JUnit Platform is supported as test framework!" }
		val collectTestwiseCoverage = impactedReports.testwiseCoverage.required.get()
		if (collectTestwiseCoverage) {
			check(maxParallelForks == 1) { "maxParallelForks is ${maxParallelForks}. Testwise coverage collection is only supported for maxParallelForks=1!" }

			(stableClasspath as ConfigurableFileCollection).from(testEngineConfiguration)

			if (runImpacted.get()) {
				// Workaround to not cause the task to fail when no tests are executed, which might happen when no tests are impacted
				// We do so by adding a useless filter, because the "no tests executed" can only be disabled for the case where filters are applied
				// https://docs.gradle.org/8.8/userguide/upgrading_version_8.html#test_task_fail_on_no_test_executed
				// https://github.com/gradle/gradle/blob/57cc16c8fbf4116a0ef0ad7b742c1a4a4e11a474/platforms/software/testing-base/src/main/java/org/gradle/api/tasks/testing/AbstractTestTask.java#L542
				filter.excludeTestsMatching("dummy-test-name")
				filter.isFailOnNoMatchingTests = false
			}

			jvmArgumentProviders.removeIf { it.javaClass.name.contains("JacocoPluginExtension") }

			agentConfiguration.get().localAgent.orNull?.let {
				jvmArgs(it.getJvmArgs())
			}

			setImpactedTestEngineOptions(testFrameworkOptions)
			testFrameworkOptions.includeEngines = setOf(IMPACTED_TEST_ENGINE)
		}
		try {
			super.executeTests()
		} finally {
			if (collectTestwiseCoverage) {
				val partial = runImpacted.get() && !runAllTests.get()
				logger.info("Generating coverage reports...")
				TestwiseCoverageReporting(
					logger,
					partial,
					stableClasspath.files,
					agentConfiguration.get().getPredicate(),
					agentConfiguration.get().destination.asFile.get(),
					impactedReports.testwiseCoverage.outputLocation.asFile.get()
				).generateTestwiseCoverageReports()
			}
		}
	}

	private infix fun String.writeProperty(value: Any?) {
		value?.let { systemProperties["teamscale.test.impacted.${this}"] = it.toString() }
	}

	private fun setImpactedTestEngineOptions(options: JUnitPlatformOptions) {
		if (runImpacted.get()) {
			check(endCommit.isPresent) { "When executing only impacted tests a reference commit must be specified in the form of endRevision or endCommit!" }
			serverConfiguration.get().validate()
			"server.url" writeProperty serverConfiguration.get().url.get()
			"server.project" writeProperty serverConfiguration.get().project.get()
			"server.userName" writeProperty serverConfiguration.get().userName.get()
			"server.userAccessToken" writeProperty serverConfiguration.get().userAccessToken.get()
			"baseline" writeProperty baseline.orNull
			"baselineRevision" writeProperty baselineRevision.orNull
		}
		check(
			partition.isPresent && partition.get().isNotBlank()
		) { "Partition is required for retrieving Test Impact Analysis results" }
		"partition" writeProperty partition.get()
		"endCommit" writeProperty endCommit.get().first
		"endRevision" writeProperty endCommit.get().second
		"repository" writeProperty repository.orNull
		"reportDirectory" writeProperty agentConfiguration.get().destination.asFile.get().absolutePath
		"agentsUrls" writeProperty agentConfiguration.get().getAllAgentUrls().joinToString(",")
		"runImpacted" writeProperty runImpacted.get()
		"runAllTests" writeProperty runAllTests.get()
		"includeAddedTests" writeProperty includeAddedTests.get()
		"includeFailedAndSkipped" writeProperty includeFailedAndSkipped.get()
		"includedEngines" writeProperty options.includeEngines.joinToString(",")
		"excludedEngines" writeProperty options.excludeEngines.joinToString(",")
	}

	/**
	 * The reports that this task potentially produces.
	 *
	 * @return The reports that this task potentially produces
	 */
	@Nested
	override fun getReports(): TestImpactedTaskReports {
		return impactedReports
	}

	/**
	 * Configures the reports that this task potentially produces.
	 *
	 * @param closure The configuration
	 * @return The reports that this task potentially produces
	 */
	override fun reports(closure: Closure<*>): TestImpactedTaskReports {
		return reports(ClosureBackedAction(closure))
	}

	/**
	 * Configures the reports that this task potentially produces.
	 *
	 * @param configureAction The configuration
	 * @return The reports that this task potentially produces
	 */
	override fun reports(configureAction: Action<in TestTaskReports?>): TestImpactedTaskReports {
		configureAction.execute(impactedReports)
		return impactedReports
	}
}

