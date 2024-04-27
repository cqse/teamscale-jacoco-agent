package com.teamscale.config

import com.teamscale.Report
import com.teamscale.client.ReportFormat
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property

/** Base configuration for all kinds of reports that we want to upload from Gradle. */
abstract class ReportConfigurationBase(private val format: ReportFormat, val project: Project, task: Task) {

    /** The partition for which artifacts are uploaded. */
    var partition: Property<String> = project.objects.property(String::class.java).convention(task.name)

    fun setPartition(partition: String) {
        this.partition.set(partition)
    }

    /** The message that shows up for the upload in Teamscale. */
    var message: Property<String> =
        project.objects.property(String::class.java).convention("${format.readableName} gradle upload")

    fun setMessage(message: String) {
        this.message.set(message)
    }

    /**
     * Whether the report should be uploaded or not.
     */
    var upload: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    fun setUpload(upload: Boolean) {
        this.upload.set(upload)
    }

    /** Returns a report specification used in the TeamscaleUploadTask. */
    fun getReport(): Report {
        return Report(
            upload = upload,
            format = format,
            reportFiles = getReportFiles(),
            message = message,
            partition = partition
        )
    }

    /**
     * Constructs a lazy evaluated FileCollection with the actual report files that should be uploaded to Teamscale.
     *  i.e. for JUnit where the destination is a directory a file tree for all included xml files should be returned.
     */
    abstract fun getReportFiles(): FileCollection
}

