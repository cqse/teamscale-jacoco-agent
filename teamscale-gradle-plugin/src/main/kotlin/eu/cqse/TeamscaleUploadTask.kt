package eu.cqse

import eu.cqse.config.Server
import eu.cqse.teamscale.client.CommitDescriptor
import eu.cqse.teamscale.client.TeamscaleClient
import org.gradle.api.DefaultTask
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
    val reports = mutableListOf<Report>()

    init {
        group = "Teamscale"
        description = "Uploads reports to Teamscale"
    }

    /** Executes the report upload. */
    @TaskAction
    fun action() {
        logger.info("Uploading to $server at $commitDescriptor...")
        val client = TeamscaleClient(server.url, server.userName, server.userAccessToken, server.project)

        for (report in reports) {
            val reportFiles = listFileTree(report.report, report.format.extension)
            logger.info("Uploading ${reportFiles.size} ${report.format.name} report(s) to partition ${report.partition}...")
            if (reportFiles.isEmpty()) {
                logger.info("Skipped empty upload!")
                continue
            }

            try {
                client.uploadReports(
                    report.format, reportFiles, commitDescriptor,
                    report.partition, "${report.message} (${report.partition})"
                )
            } catch (e: ConnectException) {
                logger.error("Upload failed (${e.message})")
                throw e
            }
        }
    }

    /** Recursively lists all files in the given directory that match the specified extension. */
    private fun listFileTree(file: File, extension: String?): Collection<File> {
        return file.walkTopDown().filter { it.isFile && it.extension.equals(extension, ignoreCase = true) }.toSet()
    }
}
