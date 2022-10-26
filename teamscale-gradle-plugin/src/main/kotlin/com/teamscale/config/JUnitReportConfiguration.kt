package com.teamscale.config

import com.teamscale.client.EReportFormat
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.Test
import java.io.File

/** Configuration for the JUnit results report. */
open class JUnitReportConfiguration(project: Project, task: Test) :
    ReportConfigurationBase(EReportFormat.JUNIT, project, task) {

    /** The destination where the report should be written to/read from. */
    var destination: DirectoryProperty = task.reports.junitXml.outputLocation

    /** Allows to set the destination as a string, which is resolved as File internally. */
    fun setDestination(destination: String) {
        this.destination.set(project.objects.directoryProperty().fileValue(project.file(destination)))
    }

    override fun getReportFiles(): FileCollection {
        return destination.asFileTree.matching {
            it.include("**/*.xml")
        }
    }
}