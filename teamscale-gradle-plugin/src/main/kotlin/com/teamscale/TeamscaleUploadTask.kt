package com.teamscale

import com.teamscale.client.TeamscaleClient
import com.teamscale.config.extension.TeamscalePluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.net.ConnectException
import java.net.SocketTimeoutException

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
        // We want to upload e.g. all JUnit test reports that go to the same partition
        // as one commit, so we group them before uploading them
        for ((key, reports) in enabledReports.groupBy { Triple(it.format, it.partition.get(), it.message.get()) }) {
            val (format, partition, message) = key
            val reportFiles = reports.flatMap { it.reportFiles.files }.filter { it.exists() }.distinct()
            logger.info("Uploading ${reportFiles.size} ${format.name} report(s) to partition $partition...")
            if (reportFiles.isEmpty()) {
                logger.info("Skipped empty upload!")
                continue
            }
            logger.debug("Uploading $reportFiles")

            try {
                // Prefer to upload to revision and fallback to branch timestamp
                val commitDescriptorOrNull = if (revision != null) null else commitDescriptor!!
                retry(3) {
                    val client =
                        TeamscaleClient(server.url, server.userName, server.userAccessToken, server.project)
                    client.uploadReports(
                        format,
                        reportFiles,
                        commitDescriptorOrNull,
                        revision,
                        partition,
                        message
                    )
                }
            } catch (e: ConnectException) {
                throw GradleException("Upload failed (${e.message})", e)
            } catch (e: SocketTimeoutException) {
                throw GradleException("Upload failed (${e.message})", e)
            }
        }
    }
}

/**
 * Retries the given block numOfRetries-times catching any thrown exceptions.
 * If none of the retries succeeded the latest catched exception is rethrown.
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
