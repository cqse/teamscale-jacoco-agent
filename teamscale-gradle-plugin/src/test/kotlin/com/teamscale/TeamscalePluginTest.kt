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
        ).contains("SUCCESS (10 tests, 4 successes, 0 failures, 6 skipped)")
    }

    @Test
    fun `impacted unit tests produce coverage`() {
        val build = build(
            "clean",
            "unitTest",
            "--impacted",
            "--run-all-tests",
            "-x",
            "unitTestReportUpload",
            "--info",
            "--stacktrace",
            "--refresh-dependencies"
        )
        assertThat(build.output).contains("BUILD SUCCESSFUL", "4 tests successful", "6 tests skipped")
        val testwiseCoverageReportFile =
            File(temporaryFolder.root, "build/reports/testwise_coverage/testwise_coverage-Unit-Tests-unitTest.json")
        assertThat(testwiseCoverageReportFile).exists()

        val testwiseCoverageReport =
            Gson().fromJson(testwiseCoverageReportFile.reader(), TestwiseCoverageReport::class.java)
        assertThat(testwiseCoverageReport)
            .containsExecutionResult("com/example/project/IgnoredJUnit4Test/systemTest", ETestExecutionResult.SKIPPED)
            .containsExecutionResult("com/example/project/JUnit4Test/systemTest", ETestExecutionResult.PASSED)
            .containsCoverage(
                "com/example/project/JUnit4Test/systemTest",
                "com/example/project/Calculator.java",
                "13,16,20-22"
            )
            .hasSize(10)
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
//            .withDebug(true)
            .build()
}

