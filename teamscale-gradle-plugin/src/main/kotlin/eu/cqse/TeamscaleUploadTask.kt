package eu.cqse

import eu.cqse.config.Server
import eu.cqse.teamscale.client.CommitDescriptor
import eu.cqse.teamscale.client.EReportFormat
import eu.cqse.teamscale.client.TeamscaleClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.ConnectException

open class TeamscaleUploadTask : DefaultTask() {
    lateinit var server: Server
    lateinit var commitDescriptor: CommitDescriptor

    private val reports = mutableListOf<Report>()

    init {
        group = "Teamscale"
        description = "Uploads reports to Teamscale"
    }

    fun addReport(report: Report) {
        reports.add(report)
    }

    @TaskAction
    fun action() {
        logger.info("Uploading to $commitDescriptor...")
        val client = TeamscaleClient(server.url, server.userName, server.userAccessToken, server.project)

        for (report in reports) {
            val reportFiles = listFileTree(report.report, report.format)
            logger.info("Uploading ${reportFiles.size} ${report.format.name} report(s) " +
                    "to $commitDescriptor (Partition ${report.partition})...")
            if (reportFiles.isEmpty()) {
                logger.info("Skipped empty upload!")
                continue
            }

            try {
                client.uploadReports(report.format, reportFiles, commitDescriptor,
                        report.partition, "${report.message} (${report.partition})")
            } catch (e: ConnectException) {
                logger.error("Upload failed (${e.message})")
                throw e
            }
        }
    }

    private fun listFileTree(file: File, format: EReportFormat): Collection<File> {
        return file.walkTopDown().filter { it.isFile && it.extension.equals(format.extension, ignoreCase = true) }.toSet()
    }
}
