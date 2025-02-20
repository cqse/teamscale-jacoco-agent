package com.teamscale

import com.teamscale.TestwiseCoverageReportAssert.Companion.assertThat
import com.teamscale.client.JsonUtils
import com.teamscale.plugin.fixtures.TeamscaleConstants
import com.teamscale.plugin.fixtures.TestRootProject
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import com.teamscale.test.commons.TeamscaleMockServer
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.management.ManagementFactory


/**
 * Integration tests for the Teamscale Gradle plugin.
 */
class TeamscalePluginTest {

	private lateinit var teamscaleMockServer: TeamscaleMockServer

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
		rootProject.withSampleCode()
		rootProject.defaultProjectSetup()
	}

	@Test
	fun `teamscale plugin can be configured`() {
		rootProject.defineLegacyUnitTestTask()

		assertThat(
			build(false, "clean", "tasks").output
		).contains("SUCCESS")
	}

	@Test
	fun `unit tests can be executed normally`() {
		rootProject.excludeFailingTests()
		rootProject.defineLegacyUnitTestTask()

		assertThat(
			build(false, "clean", "unitTest").output
		).contains("SUCCESS (18 tests, 12 successes, 0 failures, 6 skipped)")
	}

	@Test
	fun `all unit tests produce coverage`() {
		rootProject.withServerConfig()
		rootProject.defineLegacyUnitTestTask()

		val build = build(
			true,
			"--continue",
			"clean",
			"unitTest",
			"--impacted",
			"--run-all-tests",
			"teamscaleReportUpload"
		)
		assertThat(build.output).contains("FAILURE (21 tests, 14 successes, 1 failures, 6 skipped)")
			.doesNotContain("you did not provide all relevant class files")
		val testwiseCoverageReportFile =
			rootProject.buildDir.resolve("reports/testwise-coverage/unitTest/Unit-Tests.json")
		assertThat(testwiseCoverageReportFile).exists()

		assertFullCoverage(testwiseCoverageReportFile.readText())

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1)
		assertFullCoverage(teamscaleMockServer.uploadedReports[0].reportString)
		assertThat(teamscaleMockServer.uploadedReports[0].partition).isEqualTo("Unit Tests")
	}

	@Test
	fun `only impacted unit tests are executed`() {
		rootProject.withServerConfig()
		rootProject.defineLegacyUnitTestTask()

		val build = build(
			false,
			"--continue",
			"clean",
			"unitTest",
			"--impacted",
			"teamscaleReportUpload"
		)
		assertThat(build.output).contains("SUCCESS (1 tests, 1 successes, 0 failures, 0 skipped)")
		val testwiseCoverageReportFile =
			rootProject.buildDir.resolve("reports/testwise-coverage/unitTest/Unit-Tests.json")
		assertThat(testwiseCoverageReportFile).exists()

		assertPartialCoverage(testwiseCoverageReportFile.readText())

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1)
		assertPartialCoverage(teamscaleMockServer.uploadedReports[0].reportString)
		assertThat(teamscaleMockServer.uploadedReports[0].partition).isEqualTo("Unit Tests")
	}

	@Test
	fun `unit tests without server config produce coverage`() {
		rootProject.defineLegacyUnitTestTask()

		val build = build(
			true,
			"clean",
			"unitTest"
		)
		assertThat(build.output).contains("FAILURE (21 tests, 14 successes, 1 failures, 6 skipped)")
			.doesNotContain("you did not provide all relevant class files")
			.doesNotContain("WARNING: JAXBContext implementation could not be found. WADL feature is disabled.")
			.doesNotContain("WARNING: A class javax.activation.DataSource for a default provider")
		val testwiseCoverageReportFile =
			rootProject.buildDir.resolve("reports/testwise-coverage/unitTest/Unit-Tests.json")
		assertThat(testwiseCoverageReportFile).exists()

		val source = testwiseCoverageReportFile.readText()
		assertFullCoverage(source)
	}

	@Test
	fun `prefer using branch and timestamp to upload reports when provided manually`() {
		rootProject.withServerConfig()
		rootProject.withBranchAndTimestamp()
		rootProject.defineLegacyUnitTestTask()

		val build = build(
			true,
			"--continue",
			"clean",
			"unitTest",
			"--impacted",
			"--run-all-tests",
			"teamscaleReportUpload"
		)
		assertThat(teamscaleMockServer.uploadCommits).contains("null, master:1544512967526")
	}

	@Test
	fun `upload reports to repo and revision when timestamp is not provided manually`() {
		rootProject.withServerConfig()
		rootProject.defineLegacyUnitTestTask()

		val build = build(
			true,
			"--continue",
			"clean",
			"unitTest",
			"--impacted",
			"--run-all-tests",
			"teamscaleReportUpload"
		)
		assertThat(teamscaleMockServer.uploadCommits).contains("abcd1337, null")
		assertThat(teamscaleMockServer.uploadRepositories).contains("myRepoId")
	}

	@Test
	fun `prefer using branch and timestamp to retrieve impacted tests when provided manually`() {
		rootProject.withServerConfig()
		rootProject.withBranchAndTimestamp()
		rootProject.defineLegacyUnitTestTask()

		val build = build(
			false,
			"--continue",
			"clean",
			"unitTest",
			"--impacted",
			"teamscaleReportUpload"
		)
		assertThat(teamscaleMockServer.impactedTestCommits).contains("null, master:1544512967526")
	}

	@Test
	fun `use repo and revision to retrieve impacted tests when timestamp is not provided manually`() {
		rootProject.withServerConfig()
		rootProject.defineLegacyUnitTestTask()

		val build = build(
			false,
			"--continue",
			"clean",
			"unitTest",
			"--impacted",
			"teamscaleReportUpload"
		)
		assertThat(teamscaleMockServer.impactedTestCommits).contains("abcd1337, null")
		assertThat(teamscaleMockServer.impactedTestRepositories).contains("myRepoId")
	}

	@Test
	fun `wrong include pattern produces error`() {
		rootProject.defineLegacyUnitTestTask("non.existent.package.*")

		val build = build(
			true,
			"clean",
			"unitTest"
		)
		assertThat(build.output).contains("None of the 9 class files found in the given directories match the configured include/exclude patterns!")
	}

	private fun build(expectFailure: Boolean, vararg arguments: String): BuildResult {
		val runnerArgs = arguments.toMutableList()
		val runner = GradleRunner.create()
		runnerArgs.add("--stacktrace")

		if (ManagementFactory.getRuntimeMXBean().inputArguments.toString()
				.contains("-agentlib:jdwp") && arguments.contains("unitTest")
		) {
			runner.withDebug(true)
			runner.forwardOutput()
			runnerArgs.add("--refresh-dependencies")
			runnerArgs.add("--debug")
			runnerArgs.add("--debug-jvm")
		}

		runner
			.withProjectDir(rootProject.projectDir)
			.withPluginClasspath()
			.withArguments(runnerArgs)
			.withGradleVersion("8.4")

		val buildResult =
			if (expectFailure) {
				runner.buildAndFail()
			} else {
				runner.build()
			}

		return buildResult
	}

	private fun assertFullCoverage(source: String) {
		val testwiseCoverageReport = JsonUtils.deserialize(source, TestwiseCoverageReport::class.java)
		assertThat(testwiseCoverageReport)
			.hasPartial(false)
			.containsExecutionResult("com/example/project/IgnoredJUnit4Test/systemTest", ETestExecutionResult.SKIPPED)
			.containsExecutionResult("com/example/project/JUnit4Test/systemTest", ETestExecutionResult.PASSED)
			.containsExecutionResult(
				"com/example/project/JUnit5Test/withValueSource(String)",
				ETestExecutionResult.PASSED
			)
			.containsExecutionResult(
				"com/example/project/FailingRepeatedTest/testRepeatedTest()",
				ETestExecutionResult.FAILURE
			)
			.containsExecutionResult("FibonacciTest/test[4]", ETestExecutionResult.PASSED)
			.containsCoverage(
				"com/example/project/JUnit4Test/systemTest",
				"com/example/project/Calculator.java",
				"13,16,20-22"
			)
			// 19 Tests because JUnit 5 parameterized tests are grouped
			.hasSize(19)
	}

	private fun assertPartialCoverage(source: String) {
		val testwiseCoverageReport = JsonUtils.deserialize(source, TestwiseCoverageReport::class.java)
		assertThat(testwiseCoverageReport)
			.hasPartial(true)
			.containsExecutionResult("com/example/project/JUnit4Test/systemTest", ETestExecutionResult.PASSED)
			.containsCoverage(
				"com/example/project/JUnit4Test/systemTest",
				"com/example/project/Calculator.java",
				"13,16,20-22"
			)
			.hasSize(19)
			.hasTestsWithCoverage(1)
	}
}
