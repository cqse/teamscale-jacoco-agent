package eu.cqse.teamscale.jacoco.agent.store.upload.teamscale;

import eu.cqse.teamscale.client.ITeamscaleService;
import eu.cqse.teamscale.client.EReportFormat;
import eu.cqse.teamscale.client.TeamscaleServer;
import eu.cqse.teamscale.client.TeamscaleServiceGenerator;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.store.file.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.util.Benchmark;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

		api = TeamscaleServiceGenerator.createService(
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
		logger.debug("Uploading {} artifact to {}", format.readableName, teamscaleServer);

		try {
			api.uploadReport(
					teamscaleServer.project,
					teamscaleServer.commit,
					teamscaleServer.partition + format.partitionSuffix,
					format,
					teamscaleServer.message + " (" + format.readableName + ")",
					RequestBody.create(MultipartBody.FORM, xml)
			);
			return true;
		} catch (IOException e) {
			logger.error("Failed to upload coverage to {}", teamscaleServer, e);
			return false;
		}
	}

	@Override
	public String describe() {
		return "Uploading to " + teamscaleServer + " (fallback in case of network errors to: " + failureStore.describe()
				+ ")";
	}
}
