package com.teamscale

import com.teamscale.client.EReportFormat
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Report holder used to describe an already configured report
 * that should be uploaded to Teamscale.
 */
data class Report(

    @Input
    val upload: Property<Boolean>,

    /** Report format. */
    @Input
    val format: EReportFormat,

    /** The report file. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val reportFiles: FileCollection,

    /** The partition to upload the report to. */
    @Input
    var partition: String,

    /** The commit message shown in Teamscale for the upload. */
    @Input
    var message: String
)