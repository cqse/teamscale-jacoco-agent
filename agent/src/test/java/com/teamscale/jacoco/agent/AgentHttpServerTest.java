package com.teamscale.jacoco.agent;

import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.TestAgentOptionsBuilder;
import com.teamscale.jacoco.agent.upload.UploaderException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AgentHttpServerTest {

	private Agent agent;
	private final Integer httpServerPort = 8081;
	private final String defaultCommitMessage = "Some Message";
	private final String defaultPartition = "Some Partition";

	/** Starts the http server to control the agent */
	@BeforeEach
	public void setup() throws AgentOptionParseException, UploaderException {
		AgentOptions options = new TestAgentOptionsBuilder()
				.withHttpServerPort(httpServerPort)
				.withTeamscaleMessage(defaultCommitMessage)
				.withTeamscalePartition(defaultPartition)
				.create();

		// Starts the HTTP server for the agent
		agent = new Agent(options, null);
	}

	/** Stops the http server */
	@AfterEach
	public void teardown() {
		agent.stopServer();
	}

	/** Test overwriting the commit message */
	@Test
	public void testOverridingMessage() throws IOException {
		String newMessage = "New Message";

		overrideEndpoint("message", newMessage);

		TeamscaleServer teamscaleServer = agent.options.getTeamscaleServerOptions();
		assertThat(teamscaleServer.message).isEqualTo(newMessage);
	}

	/** Test reading the commit message */
	@Test
	public void testGettingMessage() throws IOException {
		String receivedMessage = readFromEndpoint("message");

		assertThat(receivedMessage).isEqualTo(defaultCommitMessage);
	}

	/** Test overwriting the partition */
	@Test
	public void testOverridingPartition() throws IOException {
		String newPartition = "New Partition";

		overrideEndpoint("partition", newPartition);

		TeamscaleServer teamscaleServer = agent.options.getTeamscaleServerOptions();
		assertThat(teamscaleServer.partition).isEqualTo(newPartition);
	}

	/** Test reading the partition */
	@Test
	public void testGettingPartition() throws IOException {
		String receivedPartition = readFromEndpoint("partition");

		assertThat(receivedPartition).isEqualTo(defaultPartition);
	}


	private void overrideEndpoint(String endpointPath, String newMessage) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
				.url("http://localhost:" + httpServerPort + "/" + endpointPath + "/" + newMessage)
				.method("POST", RequestBody.create(null, new byte[0]))
				.build();
		client.newCall(request).execute();
	}

	private String readFromEndpoint(String endpointPath) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
				.url("http://localhost:" + httpServerPort + "/" + endpointPath)
				.build();
		Response response = client.newCall(request).execute();
		return response.body() != null ? response.body().string() : "";
	}
}