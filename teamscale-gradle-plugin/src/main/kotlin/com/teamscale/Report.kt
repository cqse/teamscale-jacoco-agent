package com.teamscale

import com.teamscale.client.EReportFormat
import org.gradle.api.file.FileCollection
import java.io.File
import java.io.Serializable

/**
 * Report holder used to describe an already configured report
 * that should be uploaded to Teamscale.
 */
data class Report(

    /** Report format. */
    val format: EReportFormat,

    /** The report file. */
    val reportFile: File,

    /** The partition to upload the report to. */
    var partition: String,

    /** The commit message shown in Teamscale for the upload. */
    var message: String
) : Serializable