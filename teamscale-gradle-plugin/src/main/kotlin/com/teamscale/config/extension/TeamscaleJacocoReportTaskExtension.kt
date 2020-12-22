package com.teamscale.config.extension

import com.teamscale.config.JacocoReportConfiguration
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.Serializable

/**
 * Holds all user configuration for the teamscale plugin.
 */
open class TeamscaleJacocoReportTaskExtension(
    val project: Project,
    private val jacocoReport: JacocoReport
) : Serializable {

    val jacocoReportConfiguration: JacocoReportConfiguration? = null

    /** Configures the reports to be uploaded. */
    fun jacoco(action: Action<in JacocoReportConfiguration>) {
        jacocoReport.reports.xml.isEnabled = true
        val jacocoReportConfiguration = JacocoReportConfiguration(project, jacocoReport)
        action.execute(jacocoReportConfiguration)
    }
}