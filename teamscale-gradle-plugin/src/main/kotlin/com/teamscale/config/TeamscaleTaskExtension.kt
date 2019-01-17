package com.teamscale.config

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.io.Serializable

/**
 * Holds all user configuration for the teamscale plugin.
 */
open class TeamscaleTaskExtension(
    val project: Project,
    val parent: TeamscalePluginExtension,
    jacocoExtension: JacocoTaskExtension
) : Serializable {

    /** Settings regarding the reports to generate. */
    val report = Reports()

    /** Configures the reports to be uploaded. */
    fun report(action: Action<in Reports>) {
        action.execute(report)
    }

    /** Settings regarding the teamscale jacoco agent. */
    val agent = AgentConfiguration(project, jacocoExtension)

    /** Configures the jacoco agent options. */
    fun agent(action: Action<in AgentConfiguration>) {
        action.execute(agent)
    }

    /**
     * Merges the report configuration from TeamscalePluginExtension
     * with TeamscaleTaskExtension.
     */
    fun getMergedReports(): Reports {
        val reports = Reports()
        reports.copyWithDefault(report, parent.report)
        return reports
    }
}
