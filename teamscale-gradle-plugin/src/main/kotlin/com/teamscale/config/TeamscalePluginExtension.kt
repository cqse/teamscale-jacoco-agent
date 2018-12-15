package com.teamscale.config

import com.teamscale.TeamscalePlugin
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.process.JavaForkOptions
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.io.Serializable


/**
 * Holds all user configuration for the teamscale plugin.
 */
open class TeamscalePluginExtension(val project: Project) : Serializable {

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

    /** Creates Impacted tasks for all tests if enabled. */
    var testImpactMode: Boolean? = null

    val report = Reports()

    /** Configures the reports to be uploaded. */
    fun report(action: Action<in Reports>) {
        action.execute(report)
    }

    /**
     * @return True if all required fields have been set otherwise false.
     */
    fun validate(project: Project, testTaskName: String): Boolean {
        if (testImpactMode == true) {
            return commit.validate(project, testTaskName) && report.testwiseCoverage.validate(
                project,
                testTaskName
            )
        }
        return true
    }

    fun <T> applyTo(task: T) where T : Task, T : JavaForkOptions {
        val jacocoTaskExtension: JacocoTaskExtension? = task.extensions.getByType(JacocoTaskExtension::class.java)
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
    }
}
