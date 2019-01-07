package com.teamscale

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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