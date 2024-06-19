package com.teamscale.config

import com.teamscale.TestImpacted
import com.teamscale.client.EReportFormat
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import java.io.File

/** Configuration for the testwise coverage report. */
@Suppress("MemberVisibilityCanBePrivate")
open class TestwiseCoverageConfiguration(project: Project, task: TestImpacted) :
    ReportConfigurationBase(EReportFormat.TESTWISE_COVERAGE, project, task) {

    /** The destination where the report should be written to/read from. */
    var destination: RegularFileProperty = project.objects.fileProperty()

    /** Allows to set the destination as a string, which is resolved as File internally. */
    fun setDestination(destination: String) {
        this.destination.set(project.objects.fileProperty().fileValue(File(destination)))
    }

    init {
        destination.set(partition.map { partition ->
            project.layout.buildDirectory.file(
                "reports/testwise-coverage/${task.name}/${
                    partition.replace(
                        "[ /\\\\]".toRegex(),
                        "-"
                    )
                }.json"
            ).get()
        })
    }

    override fun getReportFiles(): FileCollection {
        return project.objects.fileCollection().from(destination)
    }
}
