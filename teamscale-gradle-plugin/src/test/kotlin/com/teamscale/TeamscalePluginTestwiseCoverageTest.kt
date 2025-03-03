package com.teamscale

import com.teamscale.client.JsonUtils
import com.teamscale.plugin.fixtures.TestwiseCoverageReportAssert.Companion.assertThat
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


/**
 * Integration tests for the Teamscale Gradle plugin focusing on the testwise coverage generation.
 */
class TeamscalePluginTestwiseCoverageTest : TeamscalePluginTestBase()  {

	@BeforeEach
	fun init() {
		rootProject.withSingleProject()
		rootProject.defaultProjectSetup()
	}

	@Test
	fun `all unit tests produce coverage`() {
		rootProject.withServerConfig()
		rootProject.defineLegacyTestTasks()
		rootProject.defineUploadTask()

		val build = runExpectingError(
			"--continue",
			"clean",
			"unitTest",
			"--impacted",
			"--run-all-tests",
			"unitTestReportUpload"
		)
		assertThat(build.output).contains("FAILURE (21 tests, 14 successes, 1 failures, 6 skipped)")
			.doesNotContain("you did not provide all relevant class files")
		val testwiseCoverageReportFile =
			rootProject.buildDir.resolve("reports/testwise-coverage/unitTestReport/testwise-coverage.json")
		assertThat(testwiseCoverageReportFile).exists()

		assertFullCoverage(testwiseCoverageReportFile.readText())

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1)
		assertFullCoverage(teamscaleMockServer.uploadedReports[0].reportString)
		assertThat(teamscaleMockServer.uploadedReports[0].partition).isEqualTo("Unit Tests")
	}

	@Test
	fun `only impacted unit tests are executed`() {
		rootProject.withServerConfig()
		rootProject.defineLegacyTestTasks()
		rootProject.defineUploadTask()

		val build = run(
			"--continue",
			"clean",
			"unitTest",
			"--impacted",
			"unitTestReportUpload"
		)
		assertThat(build.output).contains("SUCCESS (1 tests, 1 successes, 0 failures, 0 skipped)")
		val testwiseCoverageReportFile =
			rootProject.buildDir.resolve("reports/testwise-coverage/unitTestReport/testwise-coverage.json")
		assertThat(testwiseCoverageReportFile).exists()

		assertPartialCoverage(testwiseCoverageReportFile.readText())

		assertThat(teamscaleMockServer.uploadedReports).hasSize(1)
		assertPartialCoverage(teamscaleMockServer.uploadedReports[0].reportString)
		assertThat(teamscaleMockServer.uploadedReports[0].partition).isEqualTo("Unit Tests")
	}

	@Test
	fun `unit tests without server config produce coverage`() {
		rootProject.defineLegacyTestTasks()

		val build = runExpectingError(
			"clean",
			"unitTest"
		)
		assertThat(build.output).contains("FAILURE (21 tests, 14 successes, 1 failures, 6 skipped)")
			.doesNotContain("you did not provide all relevant class files")
			.doesNotContain("WARNING: JAXBContext implementation could not be found. WADL feature is disabled.")
			.doesNotContain("WARNING: A class javax.activation.DataSource for a default provider")
		val testwiseCoverageReportFile =
			rootProject.buildDir.resolve("reports/testwise-coverage/unitTestReport/testwise-coverage.json")
		assertThat(testwiseCoverageReportFile).exists()

		val source = testwiseCoverageReportFile.readText()
		assertFullCoverage(source)
	}

	@Test
	fun `wrong include pattern produces error`() {
		rootProject.defineLegacyTestTasks("non.existent.package.*")

		val build = runExpectingError(
			"clean",
			"unitTest"
		)
		// TODO Currently we scan the full classpath for classes, previously we only looked at the gradle projects within this build might be able to rebuild this via:
		// https://github.com/gradlex-org/maven-plugin-development/blob/5cab40cc4763a9471178a96ccbe37b933643506d/src/main/java/org/gradlex/maven/plugin/development/MavenPluginDevelopmentPlugin.java#L136C1-L192C6
		assertThat(build.output).contains("None of the 97").contains(" class files found in the given directories match the configured include/exclude patterns!")
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
			.containsExecutionResult("com/example/project/FibonacciTest/test[4]", ETestExecutionResult.PASSED)
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
