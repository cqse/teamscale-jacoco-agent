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

	private static final String PROXY_USER = "myProxyUser";
	private static final String PROXY_PASSWORD = "myProxyPassword";
	private static final String BASE_64_ENCODED_BASIC_AUTH = Base64.getEncoder().encodeToString((PROXY_USER + ":" + PROXY_PASSWORD).getBytes(
			StandardCharsets.UTF_8));

	@BeforeEach
	void setUp() throws IOException {
		mockProxyServer = new MockWebServer();
		mockProxyServer.start();
	}

	@Test
	void testProxyAuthentication() throws Exception {
		proxySystemProperties.setProxyHost(mockProxyServer.getHostName());
		proxySystemProperties.setProxyPort(mockProxyServer.getPort());
		proxySystemProperties.setProxyUser(PROXY_USER);
		proxySystemProperties.setProxyPassword(PROXY_PASSWORD);

		assertProxyAuthenticationIsUsed();
	}

	@Test
	void testTeamscaleProxyAuthentication() throws Exception {
		// test that the teamscale-specific options take precedence over the global ones
		proxySystemProperties.setProxyHost("incorrect");
		proxySystemProperties.setProxyPort("incorrect");
		proxySystemProperties.setProxyUser("incorrect");
		proxySystemProperties.setProxyPassword("incorrect");

		teamscaleProxySystemProperties.setProxyHost(mockProxyServer.getHostName());
		teamscaleProxySystemProperties.setProxyPort(mockProxyServer.getPort());
		teamscaleProxySystemProperties.setProxyUser(PROXY_USER);
		teamscaleProxySystemProperties.setProxyPassword(PROXY_PASSWORD);

		assertProxyAuthenticationIsUsed();
	}

	@Test
	void testMixingTeamscaleSpecificAndGlobalProxySettingsIsPossible() throws Exception {
		proxySystemProperties.setProxyHost(mockProxyServer.getHostName());
		proxySystemProperties.setProxyPort(mockProxyServer.getPort());
		proxySystemProperties.setProxyUser("incorrect");
		proxySystemProperties.setProxyPassword("incorrect");

		teamscaleProxySystemProperties.setProxyUser(PROXY_USER);
		teamscaleProxySystemProperties.setProxyPassword(PROXY_PASSWORD);

		assertProxyAuthenticationIsUsed();
	}

	@Test
	void testMixingTeamscaleSpecificAndGlobalProxySettingsIsPossibleTheOtherWayAround() throws Exception {
		proxySystemProperties.setProxyHost("incorrect");
		proxySystemProperties.setProxyPort("incorrect");
		proxySystemProperties.setProxyUser(PROXY_USER);
		proxySystemProperties.setProxyPassword(PROXY_PASSWORD);

		teamscaleProxySystemProperties.setProxyHost(mockProxyServer.getHostName());
		teamscaleProxySystemProperties.setProxyPort(mockProxyServer.getPort());

		assertProxyAuthenticationIsUsed();
	}

	@Test
	void testPartiallyMixingTeamscaleSpecificAndGlobalProxyServerSettingsIsImpossible() throws Exception {
		proxySystemProperties.setProxyHost(mockProxyServer.getHostName());
		proxySystemProperties.setProxyPort(mockProxyServer.getPort());
		proxySystemProperties.setProxyUser(PROXY_USER);
		proxySystemProperties.setProxyPassword(PROXY_PASSWORD);

		// if mixing the server settings works, reaching the host would be impossible
		teamscaleProxySystemProperties.setProxyHost("incorrect");

		assertProxyAuthenticationIsUsed();
	}

	@Test
	void testPartiallyMixingTeamscaleSpecificAndGlobalProxyAuthenticationSettingsIsImpossible() throws Exception {
		proxySystemProperties.setProxyHost(mockProxyServer.getHostName());
		proxySystemProperties.setProxyPort(mockProxyServer.getPort());
		proxySystemProperties.setProxyUser(PROXY_USER);
		proxySystemProperties.setProxyPassword(PROXY_PASSWORD);

		// if mixing the authentication settings works, authentication would not work
		teamscaleProxySystemProperties.setProxyUser("incorrect");

		assertProxyAuthenticationIsUsed();
	}

	private void assertProxyAuthenticationIsUsed() throws InterruptedException, IOException {

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
				"Basic " + BASE_64_ENCODED_BASIC_AUTH);
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