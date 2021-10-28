package com.teamscale.config.extension

import com.teamscale.config.JacocoReportConfiguration
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.Serializable

/**
 * Holds all user configuration regarding JaCoCo report uploads.
 */
open class TeamscaleJacocoReportTaskExtension(
    val project: Project,
    private val jacocoReport: JacocoReport
) : Serializable {

    val report = JacocoReportConfiguration(project, jacocoReport)

    /** Configures the reports to be uploaded. */
    fun report(action: Action<in JacocoReportConfiguration>) {
        jacocoReport.reports.xml.required.set(true)
        action.execute(report)
    }

    /** Overload for Groovy DSL compatibility. */
    fun report(closure: Closure<*>) {
        report { o -> project.configure(o, closure) }
    }
}