package com.teamscale.client;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.io.IOException;
import java.util.List;

/** {@link Retrofit} API specification for Teamscale. */
public interface ITeamscaleService {

	/**
	 * Report upload API.
	 *
	 * @param commit           A branch and timestamp to upload the report to. Can be null if revision is specified.
	 * @param moveToLastCommit Whether to move the upload timestamp to right after the last commit
	 * @param revision         This parameter allows to pass a revision instead of a timestamp. Can be null if a
	 *                         timestamp is given.
	 * @param partition        The name of the logical partition to store the results into. All existing data in this
	 *                         partition will be invalidated. A partition typically corresponds to one analysis run,
	 *                         i.e. if there are two independent builds/runs, they must use different partitions.
	 * @see <a href="https://docs.teamscale.com/howto/uploading-external-results/#upload-via-command-line">
	 * How to Upload External Analysis Results to Teamscale</a> for details.
	 */
	@Multipart
	@POST("api/v5.9.0/projects/{projectAliasOrId}/external-analysis/session/auto-create/report")
	Call<ResponseBody> uploadExternalReport(
			@Path("projectAliasOrId") String projectAliasOrId,
			@Query("format") String format,
			@Query("t") CommitDescriptor commit,
			@Query("revision") String revision,
			@Query("movetolastcommit") Boolean moveToLastCommit,
			@Query("partition") String partition,
			@Query("message") String message,
			@Part("report") RequestBody report
	);

	/**
	 * Report upload API with {@link EReportFormat}.
	 *
	 * @see #uploadExternalReport(String, String, CommitDescriptor, String, Boolean, String, String, RequestBody)
	 */
	default Call<ResponseBody> uploadExternalReport(
			String projectName,
			EReportFormat format,
			CommitDescriptor commit,
			String revision,
			Boolean moveToLastCommit,
			String partition,
			String message,
			RequestBody report
	) {
		return uploadExternalReport(projectName, format.name(), commit, revision, moveToLastCommit,
				partition, message, report);
	}

	/**
	 * Report upload API for multiple reports at once.
	 *
	 * @see #uploadExternalReport(String, String, CommitDescriptor, String, Boolean, String, String, RequestBody)
	 */
	@Multipart
	@POST("api/v5.9.0/projects/{projectName}/external-analysis/session/auto-create/report")
	Call<ResponseBody> uploadExternalReports(
			@Path("projectName") String projectName,
			@Query("format") EReportFormat format,
			@Query("t") CommitDescriptor commit,
			@Query("revision") String revision,
			@Query("movetolastcommit") boolean moveToLastCommit,
			@Query("partition") String partition,
			@Query("message") String message,
			@Part List<MultipartBody.Part> report
	);

	/**
	 * Report upload API for multiple reports at once. This is an overloaded version that takes a string as report
	 * format so that consumers can add support for new report formats without the requirement that the teamscale-client
	 * needs to be adjusted beforehand.
	 *
	 * @see #uploadExternalReport(String, String, CommitDescriptor, String, Boolean, String, String, RequestBody)
	 */
	@Multipart
	@POST("api/v5.9.0/projects/{projectName}/external-analysis/session/auto-create/report")
	Call<ResponseBody> uploadExternalReports(
			@Path("projectName") String projectName,
			@Query("format") String format,
			@Query("t") CommitDescriptor commit,
			@Query("revision") String revision,
			@Query("movetolastcommit") boolean moveToLastCommit,
			@Query("partition") String partition,
			@Query("message") String message,
			@Part List<MultipartBody.Part> report
	);

	/** Retrieve clustered impacted tests based on the given available tests and baseline timestamp. */
	@PUT("api/v8.0.0/projects/{projectName}/impacted-tests")
	Call<List<PrioritizableTestCluster>> getImpactedTests(
			@Path("projectName") String projectName,
			@Query("baseline") String baseline,
			@Query("end") CommitDescriptor end,
			@Query("partitions") List<String> partitions,
			@Query("include-non-impacted") boolean includeNonImpacted,
			@Query("include-failed-and-skipped") boolean includeFailedAndSkippedTests,
			@Query("ensure-processed") boolean ensureProcessed,
			@Query("include-added-tests") boolean includeAddedTests,
			@Body List<TestWithClusterId> availableTests
	);

	/** Retrieve unclustered impacted tests based on all tests known to Teamscale and the given baseline timestamp. */
	@GET("api/v8.0.0/projects/{projectName}/impacted-tests")
	Call<List<PrioritizableTest>> getImpactedTests(
			@Path("projectName") String projectName,
			@Query("baseline") String baseline,
			@Query("end") CommitDescriptor end,
			@Query("partitions") List<String> partitions,
			@Query("include-non-impacted") boolean includeNonImpacted,
			@Query("include-failed-and-skipped") boolean includeFailedAndSkippedTests,
			@Query("ensure-processed") boolean ensureProcessed,
			@Query("include-added-tests") boolean includeAddedTests
	);

	/**
	 * Uploads the given report body to Teamscale as blocking call with movetolastcommit set to false.
	 *
	 * @return Returns the request body if successful, otherwise throws an IOException.
	 */
	default String uploadReport(
			String projectName,
			CommitDescriptor commit,
			String revision,
			String partition,
			EReportFormat reportFormat,
			String message,
			RequestBody report
	) throws IOException {
		Boolean moveToLastCommit = false;
		if (revision != null) {
			// When uploading to a revision, we don't need commit adjustment.
			commit = null;
			moveToLastCommit = null;
		}

		try {
			Response<ResponseBody> response = uploadExternalReport(
					projectName,
					reportFormat,
					commit,
					revision,
					moveToLastCommit,
					partition,
					message,
					report
			).execute();

			ResponseBody body = response.body();
			if (response.isSuccessful()) {
				if (body == null) {
					return "";
				}
				return body.string();
			}

			String errorBody = HttpUtils.getErrorBodyStringSafe(response);
			throw new IOException(
					"Request failed with error code " + response.code() + ". Response body: " + errorBody);
		} catch (IOException e) {
			throw new IOException("Failed to upload report. " + e.getMessage(), e);
		}
	}
}