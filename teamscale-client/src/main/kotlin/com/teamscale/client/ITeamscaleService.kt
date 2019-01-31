package com.teamscale.client

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

import java.io.IOException

/** [Retrofit] API specification for Teamscale.  */
interface ITeamscaleService {

    /** Report upload API.  */
    @Multipart
    @POST("p/{projectName}/external-report/")
    fun uploadExternalReport(
        @Path("projectName") projectName: String,
        @Query("format") format: EReportFormat,
        @Query("t") commit: CommitDescriptor,
        @Query("adjusttimestamp") adjustTimestamp: Boolean,
        @Query("movetolastcommit") moveToLastCommit: Boolean,
        @Query("partition") partition: String,
        @Query("message") message: String,
        @Part("report") report: RequestBody
    ): Call<ResponseBody>

    /** Report upload API for multiple reports at once.  */
    @Multipart
    @POST("p/{projectName}/external-report/")
    fun uploadExternalReports(
        @Path("projectName") projectName: String,
        @Query("format") format: EReportFormat,
        @Query("t") commit: CommitDescriptor,
        @Query("adjusttimestamp") adjustTimestamp: Boolean,
        @Query("movetolastcommit") moveToLastCommit: Boolean,
        @Query("partition") partition: String,
        @Query("message") message: String,
        @Part report: List<MultipartBody.Part>
    ): Call<ResponseBody>

    /** Test Impact API.  */
    @PUT("p/{projectName}/test-impact")
    fun getImpactedTests(
        @Path("projectName") projectName: String,
        @Query("end") end: CommitDescriptor,
        @Query("partitions") partition: String,
        @Body report: List<TestDetails>
    ): Call<List<TestForPrioritization>>

    /** Test Impact API.  */
    @PUT("p/{projectName}/test-impact")
    fun getImpactedTests(
        @Path("projectName") projectName: String,
        @Query("baseline") baseline: Long,
        @Query("end") end: CommitDescriptor,
        @Query("partitions") partition: String,
        @Body report: List<TestDetails>
    ): Call<List<TestForPrioritization>>

    /**
     * Uploads the given report body to Teamscale as blocking call
     * with adjusttimestamp and movetolastcommit set to true.
     *
     * @return Returns the request body if successful, otherwise throws an IOException.
     */
    @Throws(IOException::class)
    fun uploadReport(
        projectName: String,
        commit: CommitDescriptor,
        partition: String,
        reportFormat: EReportFormat,
        message: String,
        report: RequestBody
    ): String {
        try {
            val response = uploadExternalReport(
                projectName,
                reportFormat,
                commit,
                true,
                false,
                partition,
                message,
                report
            ).execute()

            val body = response.body()
            if (response.isSuccessful) {
                return body!!.string()
            }

            val bodyString: String
            if (body == null) {
                bodyString = "<no body>"
            } else {
                bodyString = body.string()
            }
            throw IOException(
                "Request failed with error code " + response.code() + ". Response body:\n" + bodyString
            )
        } catch (e: IOException) {
            throw IOException("Failed to upload report. " + e.message, e)
        }

    }
}