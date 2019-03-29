package com.teamscale

import com.google.gson.Gson
import com.teamscale.TestwiseCoverageReportAssert.Companion.assertThat
import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * You must run
 * ./gradlew pluginUnderTestMetadata
 * once before running the tests in IntelliJ.
 */
class TeamscalePluginTest {

    companion object {

        /** Set this to true to enable debugging of the Gradle Plugin via port 5005. */
        private const val DEBUG_PLUGIN = false

        /** Set this to true to enable debugging of the impacted tests engine via port 5005. */
        private const val DEBUG_TEST_ENGINE = false
    }

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Before
    fun setup() {
        File("src/test/resources/calculator_groovy").copyRecursively(temporaryFolder.root)
    }

    @Test
    fun `teamscale plugin can be configured`() {
        assertThat(
            build(false, "clean", "tasks").output
        ).contains("SUCCESS")
    }

    @Test
    fun `unit tests can be executed normally`() {
        assertThat(
            build(true, "clean", "unitTest").output
        ).contains("SUCCESS (19 tests, 13 successes, 0 failures, 6 skipped)")
    }

    @Test
    fun `impacted unit tests produce coverage`() {
        val build = build(
            true,
            "clean",
            "unitTest",
            "--impacted",
            "--run-all-tests"
        )
        assertThat(build.output).contains("SUCCESS (19 tests, 13 successes, 0 failures, 6 skipped)")
            .doesNotContain("you did not provide all relevant class files")
        val testwiseCoverageReportFile =
            File(temporaryFolder.root, "build/reports/testwise_coverage/testwise_coverage-Unit-Tests-unitTest.json")
        assertThat(testwiseCoverageReportFile).exists()

        val testwiseCoverageReport =
            Gson().fromJson(testwiseCoverageReportFile.reader(), TestwiseCoverageReport::class.java)
        assertThat(testwiseCoverageReport)
            .containsExecutionResult("com/example/project/IgnoredJUnit4Test/systemTest", ETestExecutionResult.SKIPPED)
            .containsExecutionResult("com/example/project/JUnit4Test/systemTest", ETestExecutionResult.PASSED)
            .containsExecutionResult(
                "com/example/project/JUnit5Test/withValueSource(String)",
                ETestExecutionResult.PASSED
            )
            .containsExecutionResult("FibonacciTest/test[4]", ETestExecutionResult.PASSED)
            .containsCoverage(
                "com/example/project/JUnit4Test/systemTest",
                "com/example/project/Calculator.java",
                "13,16,20-22"
            )
            // 18 Tests because JUnit 5 parameterized tests are grouped
            .hasSize(18)
    }

    @Test
    fun `report directory is cleaned`() {
        val oldReportFile = File(
            temporaryFolder.root,
            "build/reports/testwise_coverage/old-report-testwise_coverage-Unit-Tests-unitTest.json"
        )
        oldReportFile.writeText("Test")
        assertThat(oldReportFile).exists()

        val build = build(
            true,
            "unitTest",
            "--impacted",
            "--run-all-tests"
        )
        assertThat(build.output).contains("SUCCESS (19 tests, 13 successes, 0 failures, 6 skipped)")
            .doesNotContain("you did not provide all relevant class files")
        val testwiseCoverageReportFile =
            File(temporaryFolder.root, "build/reports/testwise_coverage/testwise_coverage-Unit-Tests-unitTest.json")
        assertThat(testwiseCoverageReportFile).exists()
        assertThat(oldReportFile).doesNotExist()
    }

    private fun build(executesTask: Boolean, vararg arguments: String): BuildResult {
        val runnerArgs = arguments.toMutableList()
        val runner = GradleRunner.create()

        if (DEBUG_TEST_ENGINE || DEBUG_PLUGIN) {
            runner.withDebug(true)
            runnerArgs.add("--refresh-dependencies")
            runnerArgs.add("--debug")
            runnerArgs.add("--stacktrace")
        }
        if (executesTask && DEBUG_TEST_ENGINE) {
            runnerArgs.add("--debug-jvm")
        }

        runner
            .withProjectDir(temporaryFolder.root)
            .withPluginClasspath()
            .withArguments(runnerArgs)
            .withGradleVersion("4.6")

        if (DEBUG_PLUGIN) {
            runner.withDebug(true)
        }

        val buildResult = runner.build()

        if (DEBUG_TEST_ENGINE || DEBUG_PLUGIN) {
            println(buildResult.output)
        }

        return buildResult
    }
}

