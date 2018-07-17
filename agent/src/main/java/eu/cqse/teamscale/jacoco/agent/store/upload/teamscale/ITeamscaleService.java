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
		JACOCO("JaCoCo Coverage", "", "jacoco-coverage", "xml"),
		TESTWISE_COVERAGE("Testwise Coverage", "/Tests", "testwise-coverage", "xml"),
		JUNIT("JUnit Test Results", "/Test Results", "junit", "xml"),
		TEST_LIST("Test List", "/Tests", "test-list", "json");

		/** A readable name for the report type. */
		public final String readableName;

		/**
		 * The suffix that should be appended to the partition.
		 * We need this, because Teamscale marks every uniform path as deleted if uploaded to the same partition even if
		 * the upload does touch different type of data. E.g. JUnit upload will remove all file paths uploaded via JaCoCo
		 * coverage. Furthermore test details and testwise coverage need to be in the same partition.
		 */
		public  final String partitionSuffix;

		/** Prefix to use when writing the report to the file system. */
		public final String filePrefix;

		/** File extension of the report. */
		public final String extension;

		EReportFormat(String readableName, String partitionSuffix, String filePrefix, String extension) {
			this.readableName = readableName;
			this.partitionSuffix = partitionSuffix;
			this.filePrefix = filePrefix;
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