package eu.cqse

import eu.cqse.config.Server
import eu.cqse.teamscale.client.CommitDescriptor
import eu.cqse.teamscale.client.TeamscaleClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.ConnectException

/** Handles report uploads to Teamscale. */
open class TeamscaleUploadTask : DefaultTask() {

    /** The Teamscale server configuration. */
    lateinit var server: Server

    /** The commit for which the reports should be uploaded. */
    lateinit var commitDescriptor: CommitDescriptor

    /** The list of reports to be uploaded. */
    @Input
    val reports = mutableListOf<Report>()

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
            val reportFiles = reports.flatMap { listFileTree(it.report, format.extension) }
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

    /** Recursively lists all files in the given directory that match the specified extension. */
    private fun listFileTree(file: File, extension: String): Collection<File> {
        return file.walkTopDown().filter { it.isFile && it.extension.equals(extension, ignoreCase = true) }.toSet()
    }
}
