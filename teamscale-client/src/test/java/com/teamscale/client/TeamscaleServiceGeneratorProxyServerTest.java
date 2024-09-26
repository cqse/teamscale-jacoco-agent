package com.teamscale.client;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.teamscale.client.HttpUtils.PROXY_AUTHORIZATION_HTTP_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that our Retrofit + OkHttp client is using the Java proxy system properties ({@code http.proxy*}) if set
 */
class TeamscaleServiceGeneratorProxyServerTest {

	private MockWebServer mockProxyServer;
	private final ProxySystemProperties proxySystemProperties = new ProxySystemProperties(
			ProxySystemProperties.Protocol.HTTP);

	private final TeamscaleProxySystemProperties teamscaleProxySystemProperties = new TeamscaleProxySystemProperties(
			ProxySystemProperties.Protocol.HTTP);

	@BeforeEach
	void setUp() throws IOException {
		mockProxyServer = new MockWebServer();
		mockProxyServer.start();
	}


	@Test
	void testTeamscaleProxyAuthentication() throws Exception {
		String incorrectValue = "incorrect";
		// the teamscale-specific options should take precedence over the global ones
		proxySystemProperties.setProxyHost(incorrectValue);
		proxySystemProperties.setProxyPort(incorrectValue);
		proxySystemProperties.setProxyUser(incorrectValue);
		proxySystemProperties.setProxyPassword(incorrectValue);

		teamscaleProxySystemProperties.setProxyHost(mockProxyServer.getHostName());
		teamscaleProxySystemProperties.setProxyPort(mockProxyServer.getPort());

		String proxyUser = "myProxyUser";
		String proxyPassword = "myProxyPassword";
		String base64EncodedBasicAuth = Base64.getEncoder().encodeToString((proxyUser + ":" + proxyPassword).getBytes(
				StandardCharsets.UTF_8));
		teamscaleProxySystemProperties.setProxyUser(proxyUser);
		teamscaleProxySystemProperties.setProxyPassword(proxyPassword);

		assertProxyAuthenticationIsUsed(base64EncodedBasicAuth);
	}

	// TODO write a test that verifies the AgentBase correctly mixes teamscale-specific options and JVM-system properties for proxies

	private void assertProxyAuthenticationIsUsed(String base64EncodedBasicAuth) throws InterruptedException, IOException {
		ITeamscaleService service = TeamscaleServiceGenerator.createService(ITeamscaleService.class,
				HttpUrl.parse("http://localhost:1337"),
				"someUser", "someAccesstoken", HttpUtils.DEFAULT_READ_TIMEOUT,
				HttpUtils.DEFAULT_WRITE_TIMEOUT);

		// First time Retrofit/OkHttp tires without proxy auth.
		// When we return 407 Proxy Authentication Required, it retries with proxy authentication.
		mockProxyServer.enqueue(new MockResponse().setResponseCode(407));
		mockProxyServer.enqueue(new MockResponse().setResponseCode(200));
		service.sendHeartbeat("", new ProfilerInfo(new ProcessInformation("", "", 0), null)).execute();

		assertThat(mockProxyServer.getRequestCount()).isEqualTo(2);

		mockProxyServer.takeRequest(); // First request which doesn't have the proxy authentication set yet
		RecordedRequest requestWithProxyAuth = mockProxyServer.takeRequest();// Request we are actually interested in

		assertThat(requestWithProxyAuth.getHeader(PROXY_AUTHORIZATION_HTTP_HEADER)).isEqualTo(
				"Basic " + base64EncodedBasicAuth);
	}

	@AfterEach
	void tearDown() throws IOException {
		clearProxySystemProperties(proxySystemProperties);
		clearProxySystemProperties(teamscaleProxySystemProperties);

		mockProxyServer.shutdown();
		mockProxyServer.close();
	}

	private void clearProxySystemProperties(ProxySystemProperties proxySystemProperties) {
		proxySystemProperties.setProxyHost("");
		proxySystemProperties.setProxyPort("");
		proxySystemProperties.setProxyUser("");
		proxySystemProperties.setProxyPassword("");
	}
}