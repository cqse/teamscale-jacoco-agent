package com.teamscale.jacoco.agent.upload.artifactory;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.options.ArtifactoryConfig;
import com.teamscale.report.jacoco.CoverageFile;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryUploaderTest {

	@Test
	public void testUseApiKeyHeaderWhenOptionIsPresent(@TempDir File tmpDir) throws IOException, InterruptedException {
		MockWebServer mockWebServer = new MockWebServer();
		mockWebServer.enqueue(new MockResponse().setResponseCode(200));
		mockWebServer.start();
		HttpUrl serverUrl = mockWebServer.url("/artifactory/");

		ArtifactoryConfig artifactoryConfig = generateBasicArtifacotryConfig(serverUrl);
		artifactoryConfig.apiKey = "some_api_key";
		ArtifactoryUploader artifactoryUploader = new ArtifactoryUploader(artifactoryConfig, new ArrayList<>());
		File tmpFile = new File(tmpDir.getPath() + File.separator + "tmpfile");
		tmpFile.createNewFile();

		artifactoryUploader.upload(new CoverageFile(tmpFile));
		RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);

		//TODO move header to constant
		// Use constant for option
		assert recordedRequest != null;
		assertThat(recordedRequest.getHeader("X-JFrog-Art-Api")).as(
						"Artifactory API Header (X-JFrog-Art-Api) not used when the option artifactory-api-key is set.")
				.isNotNull();

		mockWebServer.shutdown();
	}

	private ArtifactoryConfig generateBasicArtifacotryConfig(HttpUrl serverUrl) {
		ArtifactoryConfig config = new ArtifactoryConfig();
		config.commitInfo = new ArtifactoryConfig.CommitInfo("some_revision", new CommitDescriptor("some_branch", 0));
		config.url = serverUrl;
		return config;
	}

}
