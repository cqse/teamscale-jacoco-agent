package com.teamscale.config.extension

import com.teamscale.config.JUnitReportConfiguration
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import java.io.Serializable

/**
 * Holds all user configuration for the teamscale plugin.
 */
open class TeamscaleTestTaskExtension(
    val project: Project,
    private val test: Test
) : Serializable {

    var jUnitReportConfiguration: JUnitReportConfiguration? = null

    /** Configures the reports to be uploaded. */
    fun junit(action: Action<in JUnitReportConfiguration>) {
        test.reports.junitXml.isEnabled = true
        this.jUnitReportConfiguration = this.jUnitReportConfiguration ?: JUnitReportConfiguration(project, test)
        action.execute(jUnitReportConfiguration!!)
    }
}