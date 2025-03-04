package com.teamscale

import com.teamscale.plugin.fixtures.TeamscaleConstants
import com.teamscale.plugin.fixtures.TestRootProject
import com.teamscale.test.commons.TeamscaleMockServer
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.management.ManagementFactory


/**
 * Integration tests for the Teamscale Gradle plugin.
 */
abstract class TeamscalePluginTestBase {

	protected lateinit var teamscaleMockServer: TeamscaleMockServer

	@BeforeEach
	fun startFakeTeamscaleServer() {
		teamscaleMockServer = TeamscaleMockServer(TeamscaleConstants.PORT)
			.withAuthentication(TeamscaleConstants.USER, TeamscaleConstants.ACCESS_TOKEN)
			.acceptingReportUploads()
			.withImpactedTests("com/example/project/JUnit4Test/systemTest")
	}

	@AfterEach
	fun serverShutdown() {
		teamscaleMockServer.shutdown()
	}

	/** The Gradle project in which the simulated checkout and test execution will happen. */
	lateinit var rootProject: TestRootProject

	@BeforeEach
	fun setup(@TempDir tempDir: File) {
		rootProject = TestRootProject(tempDir)
	}

	protected fun run(vararg arguments: String): BuildResult {
		return buildRunner(*arguments).build()
	}

	protected fun runExpectingError(vararg arguments: String): BuildResult {
		return buildRunner(*arguments).buildAndFail()
	}

	private fun buildRunner(vararg arguments: String): GradleRunner {
		val runnerArgs = arguments.toMutableList()
		val runner = GradleRunner.create()
		runner.forwardOutput()
		runnerArgs.add("--stacktrace")

		if (ManagementFactory.getRuntimeMXBean().inputArguments.toString()
				.contains("-agentlib:jdwp")
		) {
			runner.withDebug(true)
			runnerArgs.add("--refresh-dependencies")
			runnerArgs.add("--info")
			if (arguments.contains("unitTest")) {
				runnerArgs.add("--debug-jvm")
			}
		}

		runner
			.withProjectDir(rootProject.projectDir)
			.withPluginClasspath()
			.withArguments(runnerArgs)
			.withGradleVersion(TeamscalePlugin.MINIMUM_SUPPORTED_VERSION.version)

		return runner
	}
}
