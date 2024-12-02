package com.teamscale

import com.teamscale.client.EReportFormat
import com.teamscale.client.TeamscaleClient
import com.teamscale.config.extension.TeamscalePluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException

/** Handles report uploads to Teamscale. */
abstract class TeamscaleUploadTask : DefaultTask() {

    /** The global teamscale configuration. */
    @Internal
    lateinit var extension: TeamscalePluginExtension

    /** The Teamscale server configuration. */
    @get:Input
    val server
        get() = extension.server

    /** The commit for which the reports should be uploaded. */
    @get:Input
    @get:Optional
    val commitDescriptor
        get() = extension.commit.getOrResolveCommitDescriptor(project).first

    /**
     * The commit revision for which the reports should be uploaded.
     * If set it is preferred over commitDescriptor.
     */
    @get:Input
    @get:Optional
    val revision
        get() = extension.commit.getOrResolveCommitDescriptor(project).second

    /**
     * The repository id in your Teamscale project which Teamscale should use to look up the revision, if given.
     * Null or empty will lead to a lookup in all repositories in the Teamscale project.
     */
    @get:Input
    @get:Optional
    val repository
        get() = extension.repository

    /** The list of reports to be uploaded. */
    @get:Nested
    abstract val reports: SetProperty<Report>

    /** The report files. See Report.reportFiles for details. */
    @get:Input
    val reportFiles
        get() = reports.get().map { it.reportFiles }

    @Input
    var ignoreFailures: Boolean = false

    init {
        group = "Teamscale"
        description = "Uploads reports to Teamscale"
    }

    /** Executes the report upload. */
    @TaskAction
    fun action() {
        val enabledReports = reports.get().filter { it.upload.get() }

        if (enabledReports.isEmpty()) {
            logger.info("Skipping upload. No reports enabled for uploading.")
            return
        }

        server.validate()

        try {
            logger.info("Uploading to $server at ${revision ?: commitDescriptor}...")
            uploadReports(enabledReports)
        } catch (e: Exception) {
            if (ignoreFailures) {
                logger.warn("Ignoring failure during upload:")
                logger.warn(e.message, e)
            } else {
                throw e
            }
        }
    }

    private fun uploadReports(enabledReports: List<Report>) {
        // Group reports by format, partition, and message to upload similar reports together
        val groupedReports = enabledReports.groupBy {
            ReportGroupKey(it.format, it.partition.get(), it.message.get())
        }

        val teamscaleClient = server.toClient()

        groupedReports.forEach { (key, reports) ->
            val (format, partition, message) = key
            val reportFiles = getExistingReportFiles(reports)

            if (reportFiles.isEmpty()) {
                logger.info("Skipped empty upload for ${format.name} reports to partition $partition.")
                return@forEach
            }

            logger.info("Uploading ${reportFiles.size} ${format.name} report(s) to partition $partition...")
            logger.debug("Uploading {}", reportFiles)

            teamscaleClient.uploadReportFiles(format, reportFiles, partition, message)
        }
    }

    private data class ReportGroupKey(
        val format: EReportFormat,
        val partition: String,
        val message: String
    )

    private fun getExistingReportFiles(reports: List<Report>) =
        reports.flatMap { it.reportFiles.files }
            .filter { it.exists() }
            .distinct()

    private fun TeamscaleClient.uploadReportFiles(
        format: EReportFormat,
        reportFiles: List<File>,
        partition: String,
        message: String
    ) {
        val commitDescriptorOrNull = if (revision == null) commitDescriptor else null
        try {
            retry(3) {
                uploadReports(
                    format, reportFiles, commitDescriptorOrNull,
                    revision, repository, partition, message
                )
            }
        } catch (e: IOException) {
            throw GradleException("Upload failed (${e.message})", e)
        }
    }
}

/**
 * Retries the given block numOfRetries-times catching any thrown exceptions.
 * If none of the retries succeeded, the latest caught exception is rethrown.
 */
fun <T> retry(numOfRetries: Int, block: () -> T): T {
    var throwable: Throwable? = null
    (1..numOfRetries).forEach { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            throwable = e
            println("Failed attempt $attempt / $numOfRetries")
        }
    }
    throw throwable!!
}
