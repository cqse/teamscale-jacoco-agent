package com.teamscale.jacoco.agent.upload.teamscale;

import com.teamscale.client.EReportFormat;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.client.TeamscaleServiceGenerator;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.util.Benchmark;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.CoverageFile;
import org.slf4j.Logger;

import java.io.IOException;

/** Uploads XML Coverage to a Teamscale instance. */
public class TeamscaleUploader implements IUploader {

	/** The logger. */
	private final Logger logger = LoggingUtils.getLogger(this);

	/** Teamscale server details. */
	private final TeamscaleServer teamscaleServer;

	/** Constructor. */
	public TeamscaleUploader(TeamscaleServer teamscaleServer) {
		this.teamscaleServer = teamscaleServer;
	}

	@Override
	public void upload(CoverageFile coverageFile) {
		try (Benchmark benchmark = new Benchmark("Uploading report to Teamscale")) {
			if (tryUploading(coverageFile)) {
				deleteCoverageFile(coverageFile);
			} else {
				logger.warn("Failed to upload coverage to Teamscale. " +
								"Won't delete local file {} so you can upload it youself manually.",
						coverageFile);
			}
		}
	}

	private void deleteCoverageFile(CoverageFile coverageFile) {
		try {
			coverageFile.delete();
		} catch (IOException e) {
			logger.warn(
					"The upload to Teamscale was successful, but the file {} will be left on disk. " +
							"You can delete it yourself anytime - it is no longer needed.",
					coverageFile, e);
		}
	}

	/** Performs the upload and returns <code>true</code> if successful. */
	private boolean tryUploading(CoverageFile coverageFile) {
		logger.debug("Uploading JaCoCo artifact to {}", teamscaleServer);

		try {
			// Cannot be executed in the constructor as this causes issues in WildFly server (See #100)
			ITeamscaleService api = TeamscaleServiceGenerator.createService(
					ITeamscaleService.class,
					teamscaleServer.url,
					teamscaleServer.userName,
					teamscaleServer.userAccessToken
			);
			api.uploadReport(
					teamscaleServer.project,
					teamscaleServer.commit,
					teamscaleServer.revision,
					teamscaleServer.partition,
					EReportFormat.JACOCO,
					teamscaleServer.getMessage(),
					coverageFile.createFormRequestBody()
			);
			return true;
		} catch (IOException e) {
			logger.error("Failed to upload coverage to {}", teamscaleServer, e);
			return false;
		}
	}

	@Override
	public String describe() {
		return "Uploading to " + teamscaleServer;
	}
}
