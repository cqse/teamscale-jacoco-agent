package com.teamscale.tia.client

import com.teamscale.tia.client.AgentCommunicationUtils.handleRequestError
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import java.io.IOException
import java.util.function.Supplier

class AgentCommunicationUtilsTest {
	private interface ITestService {
		/** Test request.  */
		@GET("request")
		fun testRequest(): Call<ResponseBody>
	}

	@Test
	@Throws(Exception::class)
	fun shouldRetryRequestsOnce() {
		val server = MockWebServer().apply {
			enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
			enqueue(MockResponse().setResponseCode(200).setBody("result"))
		}

		val service = Retrofit.Builder().baseUrl("http://localhost:" + server.port)
			.build()
			.create(ITestService::class.java)

		// should not throw since the second call works
		handleRequestError("test") { service.testRequest() }
	}

	@Test
	@Throws(Exception::class)
	fun shouldNotRetryIfFirstRequestSucceeds() {
		val server = MockWebServer().apply {
			enqueue(MockResponse().setResponseCode(200).setBody("result"))
			enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
		}

		val service = Retrofit.Builder().baseUrl("http://localhost:" + server.port)
			.build()
			.create(ITestService::class.java)

		// should not throw since the first call works
		handleRequestError("test") { service.testRequest() }
	}

	@Test
	fun shouldNotRetryMoreThanOnce() {
		val server = MockWebServer().apply {
			enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
			enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
			enqueue(MockResponse().setResponseCode(200).setBody("result"))
		}

		val exception = Assertions.assertThrows(
			AgentHttpRequestFailedException::class.java
		) {
			val service = Retrofit.Builder().baseUrl("http://localhost:" + server.port)
				.build()
				.create(ITestService::class.java)
			handleRequestError("test") { service.testRequest() }
		}
		org.assertj.core.api.Assertions.assertThat(exception.cause).isInstanceOf(IOException::class.java)
	}
}