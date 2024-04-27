package com.teamscale.config

import com.teamscale.client.ReportFormat
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

/** Configuration for the JaCoCo coverage report. */
open class JacocoReportConfiguration(project: Project, task: JacocoReport) :
    ReportConfigurationBase(ReportFormat.JACOCO, project, task) {
    /** The destination where the report should be written to/read from. */
    var destination: RegularFileProperty = task.reports.xml.outputLocation

    /** Allows to set the destination as a string, which is resolved as File internally. */
    fun setDestination(destination: String) {
        this.destination.set(project.objects.fileProperty().fileValue(File(destination)))
    }

    override fun getReportFiles(): FileCollection {
        return project.objects.fileCollection().from(destination)
    }
}