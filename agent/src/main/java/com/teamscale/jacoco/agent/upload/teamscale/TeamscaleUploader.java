package com.teamscale.jacoco.agent.upload.teamscale;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;

import com.teamscale.client.EReportFormat;
import com.teamscale.client.HttpUtils;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.client.TeamscaleServiceGenerator;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.util.Benchmark;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.CoverageFile;

/** Uploads XML Coverage to a Teamscale instance. */
public class TeamscaleUploader implements IUploader {

	/** The properties file suffix for unsuccessful coverage uploads. */
	public static final String RETRY_UPLOAD_FILE_SUFFIX = "_upload-retry.properties";

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
				logger.warn("Failed to upload coverage to Teamscale. "
						+ "Won't delete local file {} so that the upload can automatically be retried upon profiler restart."
						+ " Upload can also be retried manually.", coverageFile);
				markFileForUploadRetry(coverageFile);
			}
		}
	}

	/**
	 * Creates a properties file that is mapped to the coverage that could not be
	 * uploaded, so that the coverage upload can be retried another time.
	 * 
	 */
	private void markFileForUploadRetry(CoverageFile coverageFile) {
		File uploadMetadataFile = new File(FileSystemUtils.replaceFilePathFilenameWith(
				com.teamscale.client.FileSystemUtils.normalizeSeparators(coverageFile.toString()),
				coverageFile.getName() + RETRY_UPLOAD_FILE_SUFFIX));

		List<String> configuration = new ArrayList<>();
		configuration.add("url=" + teamscaleServer.url.toString());
		configuration.add("project=" + teamscaleServer.project);
		configuration.add("userName=" + teamscaleServer.userName);
		configuration.add("userAccessToken=" + teamscaleServer.userAccessToken);
		configuration.add("partition=" + teamscaleServer.partition);
		configuration.add("commit=" + teamscaleServer.commit.toString());
		configuration.add("revision=" + teamscaleServer.revision);

		// we don't want to have newlines in our message to make parsing easier
		configuration.add("message=" + teamscaleServer.getMessage().replace("\n", "$nl$"));
		try {
			FileSystemUtils.writeLines(uploadMetadataFile, configuration);
		} catch (IOException e) {
			logger.warn(
					"Failed to create metadata file for automatic upload retry of {}. Please manually retry the coverage upload to Teamscale.",
					coverageFile);
			uploadMetadataFile.delete();
		}
	}

	private void deleteCoverageFile(CoverageFile coverageFile) {
		try {
			coverageFile.delete();
		} catch (IOException e) {
			logger.warn("The upload to Teamscale was successful, but the deletion of the coverage file {} failed. "
					+ "You can delete it yourself anytime - it is no longer needed.", coverageFile, e);
		}
	}

	/** Performs the upload and returns <code>true</code> if successful. */
	private boolean tryUploading(CoverageFile coverageFile) {
		logger.debug("Uploading JaCoCo artifact to {}", teamscaleServer);

		try {
			// Cannot be executed in the constructor as this causes issues in WildFly server
			// (See #100)
			ITeamscaleService api = TeamscaleServiceGenerator.createService(ITeamscaleService.class,
					teamscaleServer.url, teamscaleServer.userName, teamscaleServer.userAccessToken,
					HttpUtils.DEFAULT_READ_TIMEOUT, HttpUtils.DEFAULT_WRITE_TIMEOUT);
			api.uploadReport(teamscaleServer.project, teamscaleServer.commit, teamscaleServer.revision,
					teamscaleServer.partition, EReportFormat.JACOCO, teamscaleServer.getMessage(),
					coverageFile.createFormRequestBody());
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
