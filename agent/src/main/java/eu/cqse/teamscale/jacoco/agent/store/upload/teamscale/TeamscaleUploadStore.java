package eu.cqse.teamscale.jacoco.agent.store.upload.teamscale;

import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.store.file.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat;
import eu.cqse.teamscale.jacoco.util.Benchmark;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Response;

import java.io.IOException;

/** Uploads XML Coverage to a Teamscale instance. */
public class TeamscaleUploadStore implements IXmlStore {

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** The store to write failed uploads to. */
	private final TimestampedFileStore failureStore;

	/** Teamscale server details. */
	private final TeamscaleServer teamscaleServer;

	/** The API which performs the upload. */
	private final ITeamscaleService api;

	/** Constructor. */
	public TeamscaleUploadStore(TimestampedFileStore failureStore, TeamscaleServer teamscaleServer) {
		this.failureStore = failureStore;
		this.teamscaleServer = teamscaleServer;

		api = BasicAuthServiceGenerator.createService(
				ITeamscaleService.class,
				teamscaleServer.url,
				teamscaleServer.userName,
				teamscaleServer.userAccessToken
		);
	}

	@Override
	public void store(String xml, EReportFormat format) {
		try (Benchmark benchmark = new Benchmark("Uploading report to Teamscale")) {
			if (!tryUploading(xml, format)) {
				logger.warn("Storing failed upload in {}", failureStore.getOutputDirectory());
				failureStore.store(xml, format);
			}
		}
	}

	/** Performs the upload and returns <code>true</code> if successful. */
	private boolean tryUploading(String xml, EReportFormat format) {
		logger.debug("Uploading coverage to {}", teamscaleServer);

		try {
			Response<ResponseBody> response = api.uploadReport(
					teamscaleServer.project,
					teamscaleServer.commit,
					teamscaleServer.partition + getPartitionSuffix(format),
					format,
					teamscaleServer.message,
					RequestBody.create(MultipartBody.FORM, xml)
			).execute();
			if (response.isSuccessful()) {
				return true;
			}

			ResponseBody body = response.body();
			String bodyString;
			if (body == null) {
				bodyString = "<no body>";
			} else {
				bodyString = body.string();
			}
			logger.error("Failed to upload coverage to {}. Request failed with error code {}. Response body:\n{}",
					teamscaleServer, response.code(), bodyString);
			return false;
		} catch (IOException e) {
			logger.error("Failed to upload coverage to {}. Probably a network problem", teamscaleServer, e);
			return false;
		}
	}

	/**
	 * Returns the suffix that should be appended to the partition.
	 * We need this, because Teamscale marks every uniform path as deleted if uploaded to the same partition even if
	 * the upload does touch different type of data. E.g. JUnit upload will remove all file paths uploaded via JaCoCo
	 * coverage. Furthermore test details and testwise coverage needs to be in the same partition.
	 */
	private String getPartitionSuffix(EReportFormat format) {
		switch (format) {
			case JACOCO:
				return "";
			case TEST_LIST:
				return "/Tests";
			case TESTWISE_COVERAGE:
				return "/Tests";
			case JUNIT:
				return "/Test Results";
			default:
				return "/" + format.name();
		}
	}

	@Override
	public String describe() {
		return "Uploading to " + teamscaleServer + " (fallback in case of network errors to: " + failureStore.describe()
				+ ")";
	}
}
