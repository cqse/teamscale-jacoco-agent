package com.teamscale.config.extension

import com.teamscale.TeamscalePlugin
import com.teamscale.config.Commit
import com.teamscale.config.ServerConfiguration
import com.teamscale.config.TopLevelReportConfiguration
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.JavaForkOptions
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension


/**
 * Holds all user configuration for the teamscale plugin.
 */
open class TeamscalePluginExtension(val project: Project) {

    val server = ServerConfiguration()

    /** Configures the Teamscale server. */
    fun server(action: Action<in ServerConfiguration>) {
        action.execute(server)
    }

    /** Overload for Groovy DSL compatibility. */
    fun server(closure: Closure<*>) {
        server { this@TeamscalePluginExtension.project.configure(this, closure) }
    }

    val commit = Commit()

    /** Configures the code commit. */
    fun commit(action: Action<in Commit>) {
        action.execute(commit)
    }

    /** Overload for Groovy DSL compatibility. */
    fun commit(closure: Closure<*>) {
        commit { project.configure(this, closure) }
    }

    /**
     * Impacted tests are calculated from baseline to endCommit. This sets the baseline.
     */
    var baseline: Long? = null

    /**
     * Impacted tests are calculated from baseline to endCommit.
     * The baselineRevision sets the baseline with the help of a VCS revision (e.g. git SHA1) instead of a branch and timestamp
     */
    var baselineRevision: String? = null

    /** Configures the repository in which the baseline should be resolved in Teamscale (esp. if there's more than one repo in the Teamscale project). */
    var repository: String? = null

    val report = TopLevelReportConfiguration(project)

    /** Configures the reports to be uploaded. */
    fun report(action: Action<in TopLevelReportConfiguration>) {
        action.execute(report)
    }

    /** Overload for Groovy DSL compatibility. */
    fun report(closure: Closure<*>) {
        report { project.configure(this, closure) }
    }

    fun <T> applyTo(task: T): TeamscaleTestImpactedTaskExtension where T : Task, T : JavaForkOptions {
        val jacocoTaskExtension: JacocoTaskExtension = task.extensions.getByType<JacocoTaskExtension>()
        jacocoTaskExtension.excludes?.addAll(DEFAULT_EXCLUDES)

        val extension =
            task.extensions.create<TeamscaleTestImpactedTaskExtension>(
                TeamscalePlugin.teamscaleExtensionName,
                project,
                jacocoTaskExtension,
                task
            )
        extension.agent.setDestination(project.layout.buildDirectory.dir("jacoco/${project.name}-${task.name}"))
        return extension
    }

    companion object {
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
}

