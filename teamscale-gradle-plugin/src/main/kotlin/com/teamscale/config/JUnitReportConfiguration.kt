package com.teamscale.config

import com.teamscale.client.EReportFormat
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

/** Configuration for the JUnit results report. */
open class JUnitReportConfiguration(project: Project, task: Test) :
    ReportConfigurationBase(EReportFormat.JUNIT, project, task) {
    init {
        destination.set(task.reports.junitXml.outputLocation)
    }
}