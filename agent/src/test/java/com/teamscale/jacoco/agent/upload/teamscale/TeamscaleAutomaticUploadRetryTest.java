package com.teamscale.jacoco.agent.upload.teamscale;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.TestAgentOptionsBuilder;
import com.teamscale.jacoco.agent.upload.UploadTestBase;
import com.teamscale.jacoco.agent.upload.UploaderException;

import okhttp3.mockwebserver.MockResponse;

/**
 * Tests that the automatic reupload of previously unsuccessful coverage uploads
 * works for Teamscale.
 */
public class TeamscaleAutomaticUploadRetryTest extends UploadTestBase {

	/**
	 * Makes a failing upload attempt and then automatically retries to upload the
	 * leftover coverage on disk.
	 */
	@Test
	void testAutomaticUploadRetry() throws UploaderException {
		TeamscaleServer server = new TeamscaleServer();
		server.url = mockWebServer.url(serverUrl);
		server.project = "Fooproject";
		server.partition = "Test";
		server.commit = CommitDescriptor.parse("master:HEAD");
		server.userName = "Foo";
		server.userAccessToken = "Token";
		uploader = new TeamscaleUploader(server);
		mockWebServer.enqueue(new MockResponse().setResponseCode(400));
		// This is expected to fail and leave the coverage on disk.
		uploader.upload(coverageFile);
		assertThat(Files.exists(Paths.get(coverageFile.toString()))).isEqualTo(true);
		AgentOptions options = new TestAgentOptionsBuilder().withTeamscaleMessage("Foobar")
				.withTeamscalePartition("Test").withTeamscaleProject("project").withTeamscaleUser("User")
				.withTeamscaleUrl(mockWebServer.url(serverUrl).toString()).withTeamscaleAccessToken("foobar123")
				.withTeamscaleRevision("greatrevision").create();
		startAgentAfterUploadFailure(options);
		// A deleted coverage file tells us that the upload was successful.
		assertThat(Files.notExists(Paths.get(coverageFile.toString()))).isEqualTo(true);
	}
}
