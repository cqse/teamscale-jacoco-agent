package com.teamscale.jacoco.agent.upload.teamscale;

import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.COMMIT;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.MESSAGE;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.PARTITION;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.PROJECT;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.REVISION;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.URL;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.USER_ACCESS_TOKEN;
import static com.teamscale.jacoco.agent.upload.teamscale.ETeamscaleServerProperties.USER_NAME;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.slf4j.Logger;

import com.google.common.base.Strings;
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

	/**
	 * The properties file suffix for unsuccessful coverage uploads.
	 */
	public static final String TEAMSCALE_RETRY_UPLOAD_FILE_SUFFIX = "_teamscale-retry.properties";

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
						+ "Won't delete local file {} so that the upload can automatically be retried upon profiler restart. "
						+ "Upload can also be retried manually.", coverageFile);
				markFileForUploadRetry(coverageFile);
			}
		}
	}

	/**
	 * Creates a properties file that is mapped to the coverage that could not be
	 * uploaded, so that the coverage upload can be retried another time.
	 * 
	 */
	public void markFileForUploadRetry(CoverageFile coverageFile) {
		File uploadMetadataFile = new File(FileSystemUtils.replaceFilePathFilenameWith(
				com.teamscale.client.FileSystemUtils.normalizeSeparators(coverageFile.toString()),
				coverageFile.getName() + TEAMSCALE_RETRY_UPLOAD_FILE_SUFFIX));
		Properties serverProperties = this.createServerProperties();
		try {
			OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(uploadMetadataFile.toPath()),
					StandardCharsets.UTF_8);
			serverProperties.store(writer, null);
			writer.close();
		} catch (IOException e) {
			logger.warn(
					"Failed to create metadata file for automatic upload retry of {}. Please manually retry the coverage upload to Teamscale.",
					coverageFile);
			uploadMetadataFile.delete();
		}
	}

	/**
	 * Creates server properties to be written in a properties file.
	 */
	private Properties createServerProperties() {
		Properties serverProperties = new Properties();
		serverProperties.setProperty(URL.name(), teamscaleServer.url.toString());
		serverProperties.setProperty(PROJECT.name(), teamscaleServer.project);
		serverProperties.setProperty(USER_NAME.name(), teamscaleServer.userName);
		serverProperties.setProperty(USER_ACCESS_TOKEN.name(), teamscaleServer.userAccessToken);
		serverProperties.setProperty(PARTITION.name(), teamscaleServer.partition);
		serverProperties.setProperty(COMMIT.name(), teamscaleServer.commit.toString());
		serverProperties.setProperty(REVISION.name(), Strings.nullToEmpty(teamscaleServer.revision));
		serverProperties.setProperty(MESSAGE.name(), teamscaleServer.getMessage());
		return serverProperties;
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
