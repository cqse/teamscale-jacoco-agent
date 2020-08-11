package com.teamscale.jacoco.agent;

import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.AgentOptions;
import com.teamscale.jacoco.agent.options.AgentOptionsParser;
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
	private final String defaultCommitMessage = "Some Message";

	/** Starts the http server to control the agent */
	@BeforeEach
	public void setup() throws AgentOptionParseException, UploaderException {
		AgentOptions options = AgentOptionsParser.parse("" +
						"teamscale-server-url=127.0.0.1," +
						"teamscale-project=test," +
						"teamscale-user=build," +
						"teamscale-access-token=token," +
						"teamscale-partition=\"Unit Tests\"," +
						"teamscale-commit=default:HEAD," +
						"teamscale-message=\"" + defaultCommitMessage + "\"," +
						"http-server-port=8081",
				null);
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

		overrideMessage(newMessage);

		TeamscaleServer teamscaleServer = agent.options.getTeamscaleServerOptions();
		assertThat(teamscaleServer.message).isEqualTo(newMessage);
	}

	/** Test reading the commit message */
	@Test
	public void testGettingMessage() throws IOException {
		String receivedMessage = getMessage();

		assertThat(receivedMessage).isEqualTo(defaultCommitMessage);
	}

	private void overrideMessage(String newMessage) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
				.url("http://localhost:8081/message/" + newMessage)
				.method("POST", RequestBody.create(null, new byte[0]))
				.build();
		client.newCall(request).execute();
	}

	private String getMessage() throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
				.url("http://localhost:8081/message")
				.build();
		Response response = client.newCall(request).execute();
		return response.body() != null ? response.body().string() : "";
	}
}