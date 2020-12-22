package com.teamscale.config

import com.teamscale.Report
import com.teamscale.client.EReportFormat
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import java.io.File
import java.io.Serializable

/** Configuration for the testwise coverage report. */
open class ReportConfigurationBase(private val format: EReportFormat, val project: Project, task: Task) :
    Serializable {

    /** The partition for which artifacts are uploaded. */
    var partition: Property<String> = project.objects.property(String::class.java).value(task.name)

    /** The message that shows up for the upload in Teamscale. */
    var message: Property<String> =
        project.objects.property(String::class.java).value("${format.readableName} gradle upload")

    /**
     * Whether the report should be uploaded or not.
     * If it is null the report should not even be created.
     */
    var upload: Property<Boolean> = project.objects.property(Boolean::class.java).value(true)

    /** The destination where the report should be written to/read from. */
    var destination: Property<File> = project.objects.property(File::class.java)

    /** @see #destination */
    fun setDestination(destination: String) {
        this.destination.set(File(destination))
    }

    /** Returns a report specification used in the TeamscaleUploadTask. */
    fun getReport(): Report {
        return Report(
            format = format,
            reportFile = destination.get(),
            message = message.get(),
            partition = partition.get(),
            upload = upload.get()
        )
    }
}

