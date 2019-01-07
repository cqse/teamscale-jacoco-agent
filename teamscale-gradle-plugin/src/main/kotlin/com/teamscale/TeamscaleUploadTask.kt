package com.teamscale

import com.teamscale.client.TeamscaleClient
import com.teamscale.config.TeamscalePluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.net.ConnectException

/** Handles report uploads to Teamscale. */
open class TeamscaleUploadTask : DefaultTask() {

    @Internal
    lateinit var extension: TeamscalePluginExtension

    /** The Teamscale server configuration. */
    @get:Input
    val server
        get() = extension.server

    /** The commit for which the reports should be uploaded. */
    @get:Input
    val commitDescriptor
        get() = extension.commit.getCommitDescriptor()

    /** The list of reports to be uploaded. */
    @Input
    val reports = mutableSetOf<Report>()

    init {
        group = "Teamscale"
        description = "Uploads reports to Teamscale"
    }

    /** Executes the report upload. */
    @TaskAction
    fun uploadReports() {
        logger.info("Uploading to $server at $commitDescriptor...")
        val client = TeamscaleClient(server.url, server.userName, server.userAccessToken, server.project)

        // We want to upload e.g. all JUnit test reports that go to the same partition
        // as one commit so we group them before uploading them
        for ((key, reports) in reports.groupBy { Triple(it.format, it.partition, it.message) }) {
            val (format, partition, message) = key
            val reportFiles = reports.map { it.reportFile }.distinct()
            logger.info("Uploading ${reportFiles.size} ${format.name} report(s) to partition $partition...")
            if (reportFiles.isEmpty()) {
                logger.info("Skipped empty upload!")
                continue
            }

            try {
                client.uploadReports(
                    format, reportFiles, commitDescriptor, partition, "$message ($partition)"
                )
            } catch (e: ConnectException) {
                logger.error("Upload failed (${e.message})")
                throw e
            }
        }
    }
}
