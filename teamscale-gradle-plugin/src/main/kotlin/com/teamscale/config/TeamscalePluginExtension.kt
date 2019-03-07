package com.teamscale.config

import com.teamscale.TeamscalePlugin
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
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

    val commit = Commit()

    /** Configures the code commit. */
    fun commit(action: Action<in Commit>) {
        action.execute(commit)
    }

    val report = Reports()

    /** Configures the reports to be uploaded. */
    fun report(action: Action<in Reports>) {
        action.execute(report)
    }

    fun <T> applyTo(task: T): TeamscaleTaskExtension where T : Task, T : JavaForkOptions {
        val jacocoTaskExtension: JacocoTaskExtension = task.extensions.getByType(JacocoTaskExtension::class.java)
        jacocoTaskExtension.excludes?.addAll(DEFAULT_EXCLUDES)

        val extension =
            task.extensions.create(
                TeamscalePlugin.teamscaleExtensionName,
                TeamscaleTaskExtension::class.java,
                project,
                this,
                jacocoTaskExtension
            )
        extension.agent.setDestination(task.project.provider {
            project.file("${project.buildDir}/jacoco/${project.name}-${task.name}")
        })
        return extension
    }

    companion object {
        private val DEFAULT_EXCLUDES = listOf(
            "org.junit.*",
            "com.teamscale.jacoco.agent.*",
            "com.teamscale.test.listeners.*",
            "com.teamscale.report.*",
            "com.teamscale.client.*",
            "org.jacoco.core.*",
            "shadow.*",
            "okhttp3.*",
            "okio.*",
            "retrofit2.*",
            "*.MockitoMock.*",
            "*.FastClassByGuice.*"
        )
    }
}
