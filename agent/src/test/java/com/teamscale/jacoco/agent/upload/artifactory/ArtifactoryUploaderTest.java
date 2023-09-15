package com.teamscale.jacoco.agent.upload.artifactory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.EReportFormat;
import com.teamscale.jacoco.agent.upload.UploadTestBase;
import com.teamscale.jacoco.agent.upload.UploaderException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public class ArtifactoryUploaderTest extends UploadTestBase {

	/** Sets up the artifactory uploader with some basic credentials. */
	@BeforeEach
	void setupArtifactoryUploader() {
		HttpUrl serverUrl = mockWebServer.url("/artifactory/");
		ArtifactoryConfig artifactoryConfig = generateBasicArtifactoryConfig(serverUrl);
		artifactoryConfig.apiKey = "some_api_key";
		uploader = new ArtifactoryUploader(artifactoryConfig, new ArrayList<>(), EReportFormat.JACOCO);
	}

	/**
	 * Tests if the {@link ArtifactoryConfig#ARTIFACTORY_API_KEY_OPTION} is set, it
	 * will be used as authentication method against artifactory in the
	 * {@link ArtifactoryUploader#ARTIFACTORY_API_HEADER}
	 */
	@Test
	public void testUseApiKeyHeaderWhenOptionIsPresent() throws InterruptedException {
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));
		uploader.upload(coverageFile);
		RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
		assert recordedRequest != null;
		assertThat(recordedRequest.getHeader(ArtifactoryUploader.ARTIFACTORY_API_HEADER))
				.as("Artifactory API Header (" + ArtifactoryUploader.ARTIFACTORY_API_HEADER
						+ ") not used when the option" + ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION + "is set.")
				.isNotNull();
	}

	/**
	 * Tests that an unsuccessful upload is automatically retried if the profiler is
	 * started.
	 */
	@Test
	public void testAutomaticUploadRetry() throws UploaderException {
		mockWebServer.enqueue(new MockResponse().setResponseCode(400));
		uploader.upload(coverageFile);
		assertThat(Files.exists(Paths.get(coverageFile.toString()))).isEqualTo(true);
		startAgentAfterUploadFailure();
		assertThat(Files.notExists(Paths.get(coverageFile.toString()))).isEqualTo(true);
	}

	private ArtifactoryConfig generateBasicArtifactoryConfig(HttpUrl serverUrl) {
		ArtifactoryConfig config = new ArtifactoryConfig();
		config.commitInfo = new ArtifactoryConfig.CommitInfo("some_revision", new CommitDescriptor("some_branch", 0));
		config.url = serverUrl;
		return config;
	}
}
