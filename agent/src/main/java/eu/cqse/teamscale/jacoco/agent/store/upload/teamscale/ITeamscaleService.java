package eu.cqse.teamscale.jacoco.agent.store.upload.teamscale;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** {@link Retrofit} API specification for the {@link TeamscaleUploadStore}. */
public interface ITeamscaleService {

	/** Enum of report formats. */
	enum EReportFormat {
		JACOCO("JaCoCo Coverage", "", "xml"),
		TESTWISE_COVERAGE("Testwise Coverage", "/Tests", "xml"),
		JUNIT("JUnit Test Results", "/Test Results", "xml"),
		TEST_LIST("Test List", "/Tests", "json");

		/** File extension of the report. */
		public final String extension;

		/** A readable name for the report type. */
		public final String readableName;

		/**
		 * The suffix that should be appended to the partition.
		 * We need this, because Teamscale marks every uniform path as deleted if uploaded to the same partition even if
		 * the upload does touch different type of data. E.g. JUnit upload will remove all file paths uploaded via JaCoCo
		 * coverage. Furthermore test details and testwise coverage need to be in the same partition.
		 */
		public  final String partitionSuffix;

		EReportFormat(String readableName, String partitionSuffix, String extension) {
			this.readableName = readableName;
			this.partitionSuffix = partitionSuffix;
			this.extension = extension;
		}
	}

	/** Report upload API. */
	@Multipart
	@POST("p/{projectName}/external-report/")
	Call<ResponseBody> uploadExternalReport(
			@Path("projectName") String projectName,
			@Query("format") EReportFormat format,
			@Query("t") CommitDescriptor commit,
			@Query("adjusttimestamp") boolean adjustTimestamp,
			@Query("movetolastcommit") boolean moveToLastCommit,
			@Query("partition") String partition,
			@Query("message") String message,
			@Part("report") RequestBody report
	);

	/**
	 * Uploads the given report body to Teamscale
	 * with adjusttimestamp and movetolastcommit set to true.
	 */
	default Call<ResponseBody> uploadReport(
			String projectName,
			CommitDescriptor commit,
			String partition,
			EReportFormat reportFormat,
			String message,
			RequestBody report
	) {
		return uploadExternalReport(projectName, reportFormat, commit, true, false, partition, message, report);
	}
}