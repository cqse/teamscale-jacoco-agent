package com.teamscale.client

import com.teamscale.client.utils.HttpUtils
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import java.io.IOException

/** [Retrofit] API specification for Teamscale.  */
interface ITeamscaleService {
	/**
	 * Report upload API.
	 *
	 * @param commit           A branch and timestamp to upload the report to. Can be null if revision is specified.
	 * @param moveToLastCommit Whether to move the upload timestamp to right after the last commit
	 * @param revision         This parameter allows to pass a revision instead of a timestamp. Can be null if a
	 * timestamp is given.
	 * @param partition        The name of the logical partition to store the results into. All existing data in this
	 * partition will be invalidated. A partition typically corresponds to one analysis run,
	 * i.e., if there are two independent builds/runs, they must use different partitions.
	 * @see [How to Upload External Analysis Results to Teamscale](https://docs.teamscale.com/howto/uploading-external-results/.upload-via-command-line) for details.
	 */
	@Multipart
	@POST("api/v5.9.0/projects/{projectAliasOrId}/external-analysis/session/auto-create/report")
	fun uploadExternalReport(
		@Path("projectAliasOrId") projectAliasOrId: String?,
		@Query("format") format: String?,
		@Query("t") commit: CommitDescriptor?,
		@Query("revision") revision: String?,
		@Query("movetolastcommit") moveToLastCommit: Boolean?,
		@Query("partition") partition: String?,
		@Query("message") message: String?,
		@Part("report") report: RequestBody?
	): Call<ResponseBody>

	/**
	 * Report upload API for multiple reports at once.
	 *
	 * @see .uploadExternalReport
	 */
	@Multipart
	@POST("api/v5.9.0/projects/{projectName}/external-analysis/session/auto-create/report")
	fun uploadExternalReports(
		@Path("projectName") projectName: String?,
		@Query("format") format: ReportFormat?,
		@Query("t") commit: CommitDescriptor?,
		@Query("revision") revision: String?,
		@Query("movetolastcommit") moveToLastCommit: Boolean,
		@Query("partition") partition: String?,
		@Query("message") message: String?,
		@Part report: List<Part?>?
	): Call<ResponseBody?>?

	/**
	 * Report upload API for multiple reports at once. This is an overloaded version that takes a string as report
	 * format so that consumers can add support for new report formats without the requirement that the teamscale-client
	 * needs to be adjusted beforehand.
	 *
	 * @see .uploadExternalReport
	 */
	@Multipart
	@POST("api/v5.9.0/projects/{projectName}/external-analysis/session/auto-create/report")
	fun uploadExternalReports(
		@Path("projectName") projectName: String?,
		@Query("format") format: String?,
		@Query("t") commit: CommitDescriptor?,
		@Query("revision") revision: String?,
		@Query("movetolastcommit") moveToLastCommit: Boolean,
		@Query("partition") partition: String?,
		@Query("message") message: String?,
		@Part report: List<MultipartBody.Part>
	): Call<ResponseBody?>

	/** Retrieve clustered impacted tests based on the given available tests and baseline timestamp.  */
	@PUT("api/v8.0.0/projects/{projectName}/impacted-tests")
	fun getImpactedTests(
		@Path("projectName") projectName: String?,
		@Query("baseline") baseline: String?,
		@Query("end") end: CommitDescriptor?,
		@Query("partitions") partitions: List<String>?,
		@Query("include-non-impacted") includeNonImpacted: Boolean,
		@Query("include-failed-and-skipped") includeFailedAndSkippedTests: Boolean,
		@Query("ensure-processed") ensureProcessed: Boolean,
		@Query("include-added-tests") includeAddedTests: Boolean,
		@Body availableTests: List<TestWithClusterId>?
	): Call<List<PrioritizableTestCluster>>

	/** Retrieve unclustered impacted tests based on all tests known to Teamscale and the given baseline timestamp.  */
	@GET("api/v8.0.0/projects/{projectName}/impacted-tests")
	fun getImpactedTests(
		@Path("projectName") projectName: String?,
		@Query("baseline") baseline: String?,
		@Query("end") end: CommitDescriptor?,
		@Query("partitions") partitions: List<String>?,
		@Query("include-non-impacted") includeNonImpacted: Boolean,
		@Query("include-failed-and-skipped") includeFailedAndSkippedTests: Boolean,
		@Query("ensure-processed") ensureProcessed: Boolean,
		@Query("include-added-tests") includeAddedTests: Boolean
	): Call<List<PrioritizableTest?>?>

	/** Registers a profiler to Teamscale and returns the profiler configuration it should be started with.  */
	@POST("api/v9.4.0/running-profilers")
	fun registerProfiler(
		@Query("configuration-id") configurationId: String?,
		@Body processInformation: ProcessInformation?
	): Call<ProfilerRegistration?>?

	/** Updates the profiler infos and sets the profiler to still alive.  */
	@PUT("api/v9.4.0/running-profilers/{profilerId}")
	fun sendHeartbeat(
		@Path("profilerId") profilerId: String?,
		@Body profilerInfo: ProfilerInfo?
	): Call<ResponseBody?>?

	/** Removes the profiler identified by given ID.  */
	@DELETE("api/v9.4.0/running-profilers/{profilerId}")
	fun unregisterProfiler(@Path("profilerId") profilerId: String?): Call<ResponseBody?>?

	companion object {
		/**
		 * Uploads the given report body to Teamscale as blocking call with movetolastcommit set to false.
		 *
		 * @return Returns the request body if successful, otherwise throws an IOException.
		 */
		@Throws(IOException::class)
		fun ITeamscaleService.uploadReport(
			projectName: String?,
			commit: CommitDescriptor?,
			revision: String?,
			partition: String?,
			reportFormat: ReportFormat,
			message: String?,
			report: RequestBody?
		): String {
			var descriptor = commit
			var moveToLastCommit: Boolean? = false
			revision?.let {
				// When uploading to a revision, we don't need commit adjustment.
				descriptor = null
				moveToLastCommit = null
			}

			try {
				val response = uploadExternalReport(
					projectName,
					reportFormat.name,
					descriptor,
					revision,
					moveToLastCommit,
					partition,
					message,
					report
				).execute()

				val body = response.body()
				if (response.isSuccessful) {
					if (body == null) {
						return ""
					}
					return body.string()
				}

				val errorBody = HttpUtils.getErrorBodyStringSafe(response)
				throw IOException(
					"Request failed with error code " + response.code() + ". Response body: " + errorBody
				)
			} catch (e: IOException) {
				throw IOException("Failed to upload report. " + e.message, e)
			}
		}
	}
}
