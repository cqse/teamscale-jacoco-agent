package com.teamscale

import com.teamscale.aggregation.TestSuiteCompatibilityUtil
import com.teamscale.config.extension.TeamscalePluginExtension
import com.teamscale.config.extension.TeamscaleTestImpactedTaskExtension
import com.teamscale.utils.AgentPortGenerator
import com.teamscale.utils.BuildVersion
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.provider.Provider
import org.gradle.api.reporting.ReportingExtension
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.base.TestingExtension
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.util.GradleVersion
import javax.inject.Inject


/**
 * Root entry point for the Teamscale Gradle plugin.
 *
 * The plugin applies the Java plugin and a root extension named teamscale.
 * Each Test task configured in the project the plugin creates a new task suffixed with {@value #impactedTestsSuffix}
 * that executes the same set of tests, but additionally collects testwise coverage and executes only impacted tests.
 * Furthermore, all reports configured are uploaded to Teamscale after the tests have been executed.
 */
abstract class TeamscalePlugin : Plugin<Project> {

	companion object {

		/** The name of the extension used to configure the plugin. */
		const val TEAMSCALE_EXTENSION_NAME = "teamscale"

		/** The name of the configuration that holds the impacted test executor and its dependencies. */
		const val IMPACTED_TEST_ENGINE_CONFIGURATION_NAME = "impactedTestsEngine"

		/** The name of the configuration that holds the teamscale jacoco agent and its dependencies. */
		const val TEAMSCALE_JACOCO_AGENT_CONFIGURATION_NAME = "teamscaleJaCoCoAgent"

		private val DEFAULT_EXCLUDES = listOf(
			"org.junit.*",
			"org.gradle.*",
			"com.esotericsoftware.*",
			"com.teamscale.jacoco.agent.*",
			"com.teamscale.test_impacted.*",
			"com.teamscale.report.*",
			"com.teamscale.client.*",
			"org.jacoco.core.*",
			"shadow.*",
			"okhttp3.*",
			"okio.*",
			"retrofit2.*",
			"*.MockitoMock.*",
			"*.FastClassByGuice.*",
			"*.ConstructorAccess"
		)
	}

	@get:Inject
	protected abstract val objectFactory: ObjectFactory

	/** The version of the teamscale gradle plugin and impacted-tests-executor.  */
	private val pluginVersion = BuildVersion.pluginVersion

	private lateinit var impactedTestEngineConfiguration: Configuration

	private lateinit var teamscaleJacocoAgentConfiguration: Configuration

	/** Applies the teamscale plugin against the given project.  */
	override fun apply(project: Project) {
		if (GradleVersion.current() < GradleVersion.version("8.4")) {
			throw GradleException("The teamscale plugin requires Gradle version 8.4.0 or higher. Version detected: ${GradleVersion.current()}")
		}

		project.logger.info("Applying teamscale plugin $pluginVersion to ${project.name}")
		project.plugins.apply {
			apply(JavaPlugin::class.java)
			apply(JacocoPlugin::class.java)
			apply(ReportingBasePlugin::class.java)
			apply(JvmTestSuitePlugin::class.java)
		}

		val pluginExtension =
			project.extensions.create(TEAMSCALE_EXTENSION_NAME, TeamscalePluginExtension::class.java)

		// Add impacted tests executor to a custom configuration that will later be used to
		// create the classpath for the TestImpacted created by this plugin.
		impactedTestEngineConfiguration = project.configurations.create(IMPACTED_TEST_ENGINE_CONFIGURATION_NAME)

		// Add teamscale jacoco agent to a custom configuration that will later be used to
		// to generate testwise coverage if enabled.
		teamscaleJacocoAgentConfiguration = project.configurations.create(TEAMSCALE_JACOCO_AGENT_CONFIGURATION_NAME)

		project.dependencies {
			impactedTestEngineConfiguration("com.teamscale:impacted-test-engine:$pluginVersion")
			teamscaleJacocoAgentConfiguration("com.teamscale:teamscale-jacoco-agent:$pluginVersion")
		}

		project.tasks.withType<TeamscaleUpload> {
			serverConfiguration.convention(pluginExtension.server)
			repository.convention(pluginExtension.repository)
			commitDescriptorOrRevision.convention(pluginExtension.commit.combine())
			ignoreFailures.convention(false)
			message.convention(partition.map { "$it Gradle upload" })
		}

		// Add the teamscale extension also to all TestImpacted tasks
		extendTestImpactedTasks(project, pluginExtension)

		// Auto-expose JUnit reports for test tasks bound to JVM test suites
		val testing = project.extensions.getByType<TestingExtension>()
		testing.suites.withType<JvmTestSuite> {
			val suite = this
			suite.targets.configureEach {
				TestSuiteCompatibilityUtil.exposeJUnitReportsForAggregation(testTask.get(), suite.name)
			}
		}
	}

	private fun extendTestImpactedTasks(
		project: Project,
		teamscalePluginExtension: TeamscalePluginExtension
	) {
		val reporting = project.extensions.getByType<ReportingExtension>()
		val agentPortGenerator: Provider<AgentPortGenerator> = project.gradle.sharedServices.registerIfAbsent(
			"agent-port-generator",
			AgentPortGenerator::class.java
		) {}
		project.tasks.withType<TestImpacted> {
			val jacocoTaskExtension = this.extensions.getByType<JacocoTaskExtension>()
			jacocoTaskExtension.excludes?.addAll(DEFAULT_EXCLUDES)

			val extension = this.extensions.create<TeamscaleTestImpactedTaskExtension>(
				TEAMSCALE_EXTENSION_NAME,
				objectFactory,
				teamscaleJacocoAgentConfiguration,
				jacocoTaskExtension
			)
			val port = agentPortGenerator.get().getNextPort()
			extension.agent.useLocalAgent("http://127.0.0.1:${port}/")
			extension.agent.destination.set(project.layout.buildDirectory.dir("jacoco/${project.name}-${this.name}"))

			runImpacted.convention(false)
			runAllTests.convention(false)
			includeAddedTests.convention(true)
			includeFailedAndSkipped.convention(true)
			agentConfiguration.convention(extension.agent)
			serverConfiguration.convention(teamscalePluginExtension.server)
			testEngineConfiguration.from(impactedTestEngineConfiguration)
			endCommit.convention(teamscalePluginExtension.commit.combine())
			baseline.convention(teamscalePluginExtension.baseline)
			baselineRevision.convention(teamscalePluginExtension.baselineRevision)
			repository.convention(teamscalePluginExtension.repository)

			reports.testwiseCoverage.required.convention(true)
			reports.testwiseCoverage.outputLocation.convention {
				reporting.baseDirectory.file(
					"testwise-coverage/${name}/${
						partition.get().replace(
							"[ /\\\\]".toRegex(),
							"-"
						)
					}.json"
				).get().asFile
			}
		}
	}
}

