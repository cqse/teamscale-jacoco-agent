package com.teamscale.jacoco.agent.store.upload.teamscale

import com.teamscale.client.EReportFormat
import com.teamscale.client.ITeamscaleService
import com.teamscale.client.TeamscaleServer
import com.teamscale.client.TeamscaleServiceGenerator
import com.teamscale.jacoco.agent.store.IXmlStore
import com.teamscale.jacoco.agent.store.file.TimestampedFileStore
import com.teamscale.jacoco.util.Benchmark
import com.teamscale.jacoco.util.LoggingUtils
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.slf4j.Logger

import java.io.IOException

/** Uploads XML Coverage to a Teamscale instance.  */
class TeamscaleUploadStore
/** Constructor.  */
    (
    /** The store to write failed uploads to.  */
    private val failureStore: TimestampedFileStore,
    /** Teamscale server details.  */
    private val teamscaleServer: TeamscaleServer
) : IXmlStore {

    /** The logger.  */
    private val logger = LoggingUtils.getLogger(this)

    /** The API which performs the upload.  */
    private val api: ITeamscaleService

    init {

        api = TeamscaleServiceGenerator.createService(
            ITeamscaleService::class.java,
            teamscaleServer.url!!,
            teamscaleServer.userName!!,
            teamscaleServer.userAccessToken!!
        )
    }

    override fun store(xml: String) {
        Benchmark("Uploading report to Teamscale").use {
            if (!tryUploading(xml)) {
                logger.warn("Storing failed upload in {}", failureStore.outputDirectory)
                failureStore.store(xml)
            }
        }
    }

    /** Performs the upload and returns `true` if successful.  */
    private fun tryUploading(xml: String): Boolean {
        logger.debug("Uploading JaCoCo artifact to {}", teamscaleServer)

        try {
            api.uploadReport(
                teamscaleServer.project!!,
                teamscaleServer.commit!!,
                teamscaleServer.partition!!,
                EReportFormat.JACOCO,
                teamscaleServer.message,
                RequestBody.create(MultipartBody.FORM, xml)
            )
            return true
        } catch (e: IOException) {
            logger.error("Failed to upload coverage to {}", teamscaleServer, e)
            return false
        }

    }

    override fun describe(): String {
        return ("Uploading to " + teamscaleServer + " (fallback in case of network errors to: " + failureStore.describe()
                + ")")
    }
}
