package com.teamscale.client;

import java.io.IOException;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** {@link Retrofit} API specification for Teamscale. */
public interface ITeamscaleService {

	/** Report upload API. */
	@Multipart
	@POST("p/{projectName}/external-report/")
	Call<ResponseBody> uploadExternalReport(
			@Path("projectName") String projectName,
			@Query("format") String format,
			@Query("t") CommitDescriptor commit,
			@Query("revision") String revision, 
			@Query("adjusttimestamp") Boolean adjustTimestamp,
			@Query("movetolastcommit") Boolean moveToLastCommit,
			@Query("partition") String partition,
			@Query("message") String message,
			@Part("report") RequestBody report
	);

	/** Report upload API with {@link EReportFormat}. */
	default Call<ResponseBody> uploadExternalReport(
			String projectName,
			EReportFormat format,
			CommitDescriptor commit,
			String revision,
			Boolean adjustTimestamp,
			Boolean moveToLastCommit,
			String partition,
			String message,
			RequestBody report
	) {
		return uploadExternalReport(projectName, format.name(), commit, revision, adjustTimestamp, moveToLastCommit,
				partition,
				message, report);
	}

	/** Report upload API for multiple reports at once. */
	@Multipart
	@POST("p/{projectName}/external-report/")
	Call<ResponseBody> uploadExternalReports(
			@Path("projectName") String projectName,
			@Query("format") EReportFormat format,
			@Query("t") CommitDescriptor commit,
			@Query("adjusttimestamp") boolean adjustTimestamp,
			@Query("movetolastcommit") boolean moveToLastCommit,
			@Query("partition") String partition,
			@Query("message") String message,
			@Part List<MultipartBody.Part> report
	);

	/** Test Impact API. */
	@PUT("p/{projectName}/test-impact")
	Call<List<PrioritizableTestCluster>> getImpactedTests(
			@Path("projectName") String projectName,
			@Query("end") CommitDescriptor end,
			@Query("partitions") String partition,
			@Query("includeNonImpacted") boolean includeNonImpacted,
			@Body List<ClusteredTestDetails> report
	);

	/** Test Impact API. */
	@PUT("p/{projectName}/test-impact")
	Call<List<PrioritizableTestCluster>> getImpactedTests(
			@Path("projectName") String projectName,
			@Query("baseline") long baseline,
			@Query("end") CommitDescriptor end,
			@Query("partitions") String partition,
			@Query("includeNonImpacted") boolean includeNonImpacted,
			@Body List<ClusteredTestDetails> report
	);

	/**
	 * Uploads the given report body to Teamscale as blocking call with adjusttimestamp 
	 * set to true and and movetolastcommit set to false.
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
		Boolean adjustTimestamp = true;
		Boolean moveToLastCommit = false;
		if(revision != null) {
			// When uploading to a revision, we don't need commit adjustment.
			commit = null;
			adjustTimestamp = null;
			moveToLastCommit = null;
		}
		
		try {
			Response<ResponseBody> response = uploadExternalReport(
					projectName,
					reportFormat,
					commit,
					revision,
					adjustTimestamp,
					moveToLastCommit,
					partition,
					message,
					report
			).execute();

			ResponseBody body = response.body();
			if (response.isSuccessful()) {
				return body.string();
			}

			String bodyString;
			if (body == null) {
				bodyString = "<no body>";
			} else {
				bodyString = body.string();
			}
			throw new IOException(
					"Request failed with error code " + response.code() + ". Response body:\n" + bodyString);
		} catch (IOException e) {
			throw new IOException("Failed to upload report. " + e.getMessage(), e);
		}
	}
}