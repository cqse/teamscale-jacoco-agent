package com.teamscale

import com.teamscale.client.EReportFormat
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

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

    /**
     * The report files.
     *
     * Gradle currently fails to pick up the producer tasks of nested provider properties resulting in TS-31797
     * (see https://github.com/gradle/gradle/issues/6619).
     * As a workaround we use @Internal here and explicitly unwrap the reportFiles in TeamscaleUploadTask.getReportFiles.
     */
    @Internal
    val reportFiles: FileCollection,

    /** The partition to upload the report to. */
    @Input
    var partition: String,

    /** The commit message shown in Teamscale for the upload. */
    @Input
    var message: String,

    /** Whether the report only contains partial data (subset of tests). Only relevant for TESTWISE_COVERAGE. */
    @Input
    var partial: Boolean = false
)