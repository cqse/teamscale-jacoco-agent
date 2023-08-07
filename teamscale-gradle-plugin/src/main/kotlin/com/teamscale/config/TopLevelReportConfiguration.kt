package com.teamscale.config

import com.teamscale.TestImpacted
import com.teamscale.config.extension.TeamscaleJacocoReportTaskExtension
import com.teamscale.config.extension.TeamscaleTestImpactedTaskExtension
import com.teamscale.config.extension.TeamscaleTestTaskExtension
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Provides some utility methods that users of the plugin can use
 * in their scripts to set properties for all tasks of a certain type.
 */
class TopLevelReportConfiguration(val project: Project) {

    /** Configures settings for all Testwise Coverage reports. */
    @JvmOverloads
    fun testwiseCoverage(action: Action<in TestwiseCoverageConfiguration> = Action {}) {
        project.tasks.withType<TestImpacted> {
            val testImpactedExtension =
                this.extensions.getByType<TeamscaleTestImpactedTaskExtension>()
            testImpactedExtension.report(action)
        }
    }

    /** Overload for Groovy DSL compatibility. */
    fun testwiseCoverage(closure: Closure<*>) {
        testwiseCoverage { project.configure(this, closure) }
    }

    /**
     * Configures JaCoCo report settings for all JaCoCo report tasks
     * and makes sure XML report generation is enabled.
     */
    @JvmOverloads
    fun jacoco(action: Action<in JacocoReportConfiguration> = Action {}) {
        project.tasks.withType<JacocoReport> {
            val testExtension = extensions.getByType<TeamscaleJacocoReportTaskExtension>()
            testExtension.report(action)
        }
    }

    /** Overload for Groovy DSL compatibility. */
    fun jacoco(closure: Closure<*>) {
        jacoco { project.configure(this, closure) }
    }

    /**
     * Configures JUnit report settings for all test tasks
     * and makes sure XML report generation is enabled.
     */
    @JvmOverloads
    fun junit(action: Action<in JUnitReportConfiguration> = Action {}) {
        project.tasks.withType<Test> {
            if (this is TestImpacted) {
                return@withType
            }
            val testExtension = this.extensions.getByType<TeamscaleTestTaskExtension>()
            testExtension.report(action)
        }
    }

    /** Overload for Groovy DSL compatibility. */
    fun junit(closure: Closure<*>) {
        junit { project.configure(this, closure) }
    }
}