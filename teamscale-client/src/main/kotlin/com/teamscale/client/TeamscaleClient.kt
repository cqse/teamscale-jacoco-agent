package com.teamscale.client

import okhttp3.HttpUrl
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException

/** Helper class to interact with Teamscale.  */
class TeamscaleClient
/** Constructor.  */
    (
    baseUrl: String, user: String, accessToken: String,
    /** The project ID within Teamscale.  */
    private val projectId: String
) {

    /** Teamscale service implementation.  */
    private val service: ITeamscaleService

    init {
        service = TeamscaleServiceGenerator
            .createService(ITeamscaleService::class.java, HttpUrl.parse(baseUrl), user, accessToken)
    }

    /**
     * Tries to retrieve the impacted tests from Teamscale.
     *
     * @return A list of external IDs to execute or null in case Teamscale did not find a test details upload for the given commit.
     */
    @Throws(IOException::class)
    fun getImpactedTests(
        testList: List<TestDetails>,
        baseline: Long?,
        endCommit: CommitDescriptor,
        partition: String
    ): Response<List<TestForPrioritization>> {
        return if (baseline == null) {
            service
                .getImpactedTests(projectId, endCommit, partition, testList)
                .execute()
        } else {
            service
                .getImpactedTests(projectId, baseline, endCommit, partition, testList)
                .execute()
        }
    }

    /** Uploads multiple reports to Teamscale.  */
    @Throws(IOException::class)
    fun uploadReports(
        reportFormat: EReportFormat,
        reports: Collection<File>,
        commitDescriptor: CommitDescriptor,
        partition: String,
        message: String
    ) {
        val partList = reports.map { file ->
            val requestBody = RequestBody.create(MultipartBody.FORM, file)
            MultipartBody.Part.createFormData("report", file.name, requestBody)
        }

        val response = service
            .uploadExternalReports(
                projectId, reportFormat, commitDescriptor, true, true, partition, message,
                partList
            ).execute()
        if (!response.isSuccessful) {
            throw IOException(response.errorBody().string())
        }
    }
}
