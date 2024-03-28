package com.teamscale.profiler.installer.utils;

import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.TeamscaleCredentials;
import okhttp3.HttpUrl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TeamscaleUtilsTest {

	private static MockTeamscale mockTeamscale;
	private static final int TEAMSCALE_PORT = 8758;

	@BeforeAll
	static void startMockTeamscale() {
		mockTeamscale = new MockTeamscale(TEAMSCALE_PORT);
	}

	@AfterAll
	static void stopMockTeamscale() {
		mockTeamscale.shutdown();
	}

	@Test
	void unresolvableUrl() {
		Assertions.assertThatThrownBy(() ->
				checkTeamscaleConnection("http://totally.invalid:9999")
		).hasMessageContaining("The host http://totally.invalid:9999/ could not be resolved");
	}

	@Test
	void unreachableUrl() {
		Assertions.assertThatThrownBy(() ->
				checkTeamscaleConnection("http://localhost:9999")
		).hasMessageContaining("The host http://localhost:9999/ refused a connection");
	}

	@Test
	void incorrectCredentials() {
		mockTeamscale.setStatusCode(401);
		Assertions.assertThatThrownBy(() ->
				checkTeamscaleConnection("http://localhost:" + TEAMSCALE_PORT)
		).hasMessageContaining("You provided incorrect credentials");
	}

	private void checkTeamscaleConnection(String url) throws FatalInstallerError {
		TeamscaleUtils.checkTeamscaleConnection(new TeamscaleCredentials(HttpUrl.get(url), "user", "key"));
	}

}