package com.teamscale.config

import com.teamscale.client.EReportFormat
import org.gradle.api.Project
import org.gradle.testing.jacoco.tasks.JacocoReport

/** Configuration for the JaCoCo coverage report. */
open class JacocoReportConfiguration(project: Project, task: JacocoReport) :
    ReportConfigurationBase(EReportFormat.JACOCO, project, task) {
    init {
        destination.set(task.reports.xml.outputLocation)
    }
}