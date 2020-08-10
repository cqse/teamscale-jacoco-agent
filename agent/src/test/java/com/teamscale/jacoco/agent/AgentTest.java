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
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AgentTest {
	/**
	 * TODO
	 *
	 * @throws AgentOptionParseException
	 * @throws UploaderException
	 * @throws IOException
	 */
	@Test
	public void testOverridingMessage() throws AgentOptionParseException, UploaderException, IOException {
		String oldMessage = "Old Message";
		String newMessage = "New Message";
		Agent agent = startAgentHttpServerWithMessage(oldMessage);

		overrideMessage(newMessage);

		TeamscaleServer teamscaleServer = agent.options.getTeamscaleServerOptions();
		assertThat(teamscaleServer.message).isEqualTo(newMessage);
	}

	/**
	 * TODO Docu + Fix running both tests (bind exception of port 8081)
	 *
	 * @throws AgentOptionParseException
	 * @throws UploaderException
	 * @throws IOException
	 */
	@Test
	public void testGetMessage() throws AgentOptionParseException, UploaderException, IOException {
		String configuredMessage = "Old Message";
		Agent agent = startAgentHttpServerWithMessage(configuredMessage);

		String receivedMessage = getMessageViaHttp();

		assertThat(receivedMessage).isEqualTo(configuredMessage);
	}

	private void overrideMessage(String newMessage) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
				.url("http://localhost:8081/message/" + newMessage)
				.method("POST", RequestBody.create(null, new byte[0]))
				.build();
		client.newCall(request).execute();
	}

	private Agent startAgentHttpServerWithMessage(String message) throws UploaderException, AgentOptionParseException {
		AgentOptions options = AgentOptionsParser.parse("" +
						"teamscale-server-url=127.0.0.1," +
						"teamscale-project=test," +
						"teamscale-user=build," +
						"teamscale-access-token=token," +
						"teamscale-partition=\"Unit Tests\"," +
						"teamscale-commit=default:HEAD," +
						"teamscale-message=\"" + message + "\"," +
						"http-server-port=8081",
				null);
		// Starts the HTTP server for the agent
		return new Agent(options, null);
	}

	private String getMessageViaHttp() throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
				.url("http://localhost:8081/message")
				.build();
		Response response = client.newCall(request).execute();
		return response.body() != null ? response.body().string() : "";
	}
}