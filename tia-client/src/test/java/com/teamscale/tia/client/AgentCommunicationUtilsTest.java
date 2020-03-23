package com.teamscale.tia.client;

import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AgentCommunicationUtilsTest {

	public interface ITestService {
		@GET("request")
		Call<ResponseBody> testRequest();
	}

	@Test
	public void shouldRetryRequestsOnce() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
		server.enqueue(new MockResponse().setResponseCode(200).setBody("result"));

		ITestService service = new Retrofit.Builder().baseUrl("http://localhost:" + server.getPort()).build()
				.create(ITestService.class);

		// should not throw since the second call works
		AgentCommunicationUtils.handleRequestError(service::testRequest, "test");
	}

	@Test
	public void shouldNotRetryIfFirstRequestSucceeds() throws Exception {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setResponseCode(200).setBody("result"));
		server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

		ITestService service = new Retrofit.Builder().baseUrl("http://localhost:" + server.getPort()).build()
				.create(ITestService.class);

		// should not throw since the first call works
		AgentCommunicationUtils.handleRequestError(service::testRequest, "test");
	}

	@Test
	public void shouldNotRetryMoreThanOnce() {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
		server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
		server.enqueue(new MockResponse().setResponseCode(200).setBody("result"));

		ITestService service = new Retrofit.Builder().baseUrl("http://localhost:" + server.getPort()).build()
				.create(ITestService.class);

		AgentHttpRequestFailedException exception = assertThrows(AgentHttpRequestFailedException.class,
				() -> AgentCommunicationUtils.handleRequestError(service::testRequest, "test"));
		assertThat(exception.getCause()).isInstanceOf(IOException.class);
	}

}