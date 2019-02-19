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
            build("clean", "tasks").output
        ).contains("SUCCESS")
    }

    @Test
    fun `unit tests can be executed normally`() {
        assertThat(
            build("clean", "unitTest").output
        ).contains("SUCCESS (19 tests, 13 successes, 0 failures, 6 skipped)")
    }

    @Test
    fun `impacted unit tests produce coverage`() {
        val build = build(
            "clean",
            "unitTest",
            "--impacted",
            "--run-all-tests",
            // Uncomment to remote debug the test engine.
            "--debug-jvm",
            "--debug",
            "--stacktrace"
        )
        assertThat(build.output).contains("SUCCESS (19 tests, 13 successes, 0 failures, 6 skipped)")
            //.doesNotContain("you did not provide all relevant class files")
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

    /**
     * Useful switches:
     * --refresh-dependencies
     * --debug-jvm
     * */

    private
    fun build(vararg arguments: String): BuildResult =
        GradleRunner
            .create()
            .withProjectDir(temporaryFolder.root)
            .withPluginClasspath()
            .withArguments(*arguments)
            .withGradleVersion("4.6")
            // Uncomment to debug the Gradle plugin
            // .withDebug(true)
            .build()
}

