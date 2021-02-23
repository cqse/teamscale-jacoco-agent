package com.teamscale.config

import com.teamscale.TestImpacted
import com.teamscale.config.extension.TeamscaleJacocoReportTaskExtension
import com.teamscale.config.extension.TeamscaleTestImpactedTaskExtension
import com.teamscale.config.extension.TeamscaleTestTaskExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Provides some utility methods that users of the plugin can use
 * in their scripts to set properties for all tasks of a certain type.
 */
class TopLevelReportConfiguration(val project: Project) {

    /** Configures settings for all Testwise Coverage reports. */
    @JvmOverloads
    fun testwiseCoverage(action: Action<in TestwiseCoverageConfiguration> = Action {}) {
        project.tasks.withType(TestImpacted::class.java) { testImpacted ->
            val testImpactedExtension =
                testImpacted.extensions.getByType(TeamscaleTestImpactedTaskExtension::class.java)
            testImpactedExtension.report(action)
        }
    }

    /**
     * Configures JaCoCo report settings for all JaCoCo report tasks
     * and makes sure XML report generation is enabled.
     */
    @JvmOverloads
    fun jacoco(action: Action<in JacocoReportConfiguration> = Action {}) {
        project.tasks.withType(JacocoReport::class.java) { jacocoReport ->
            val testExtension = jacocoReport.extensions.getByType(TeamscaleJacocoReportTaskExtension::class.java)
            testExtension.report(action)
        }
    }

    /**
     * Configures JUnit report settings for all test tasks
     * and makes sure XML report generation is enabled.
     */
    @JvmOverloads
    fun junit(action: Action<in JUnitReportConfiguration> = Action {}) {
        project.tasks.withType(Test::class.java) { test ->
            if (test is TestImpacted) {
                return@withType
            }
            val testExtension = test.extensions.getByType(TeamscaleTestTaskExtension::class.java)
            testExtension.report(action)
        }
    }
}