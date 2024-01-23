package com.teamscale.jacoco.agent.options;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamscalePropertiesUtilsTest {

	private Path teamscalePropertiesPath;

	@BeforeEach
	void createTempPath(@TempDir Path tempDir) {
		teamscalePropertiesPath = tempDir.resolve("teamscale.properties");
	}

	@Test
	void pathDoesNotExist() throws AgentOptionParseException {
		assertThat(
				TeamscalePropertiesUtils.parseCredentials(Paths.get("/does/not/exist/teamscale.properties"))).isNull();
	}

	@Test
	void successfulParsing() throws AgentOptionParseException, IOException {
		Files.write(teamscalePropertiesPath,
				"url=http://test\nusername=user\naccesskey=key".getBytes(StandardCharsets.UTF_8));

		TeamscaleCredentials credentials = TeamscalePropertiesUtils.parseCredentials(teamscalePropertiesPath);
		assertThat(credentials).isNotNull();
		assertThat(credentials.url).isEqualTo(HttpUrl.get("http://test"));
		assertThat(credentials.userName).isEqualTo("user");
		assertThat(credentials.accessKey).isEqualTo("key");
	}

	@Test
	void missingUsername() throws IOException {
		Files.write(teamscalePropertiesPath, "url=http://test\naccesskey=key".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(
				() -> TeamscalePropertiesUtils.parseCredentials(teamscalePropertiesPath)).hasMessageContaining("missing the username");
	}

	@Test
	void missingAccessKey() throws IOException {
		Files.write(teamscalePropertiesPath, "url=http://test\nusername=user".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(
				() -> TeamscalePropertiesUtils.parseCredentials(teamscalePropertiesPath)).hasMessageContaining("missing the accesskey");
	}

	@Test
	void missingUrl() throws IOException {
		Files.write(teamscalePropertiesPath, "username=user\nusername=user".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(
				() -> TeamscalePropertiesUtils.parseCredentials(teamscalePropertiesPath)).hasMessageContaining("missing the url");
	}

	@Test
	void malformedUrl() throws IOException {
		Files.write(teamscalePropertiesPath, "url=$$**\nusername=user\nusername=user".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(
				() -> TeamscalePropertiesUtils.parseCredentials(teamscalePropertiesPath)).hasMessageContaining("malformed URL");
	}

	@Test
	void fileNotReadable() throws IOException {
		Files.write(teamscalePropertiesPath, "url=http://test\nusername=user\nusername=user".getBytes(StandardCharsets.UTF_8));
		assertThat(teamscalePropertiesPath.toFile().setReadable(false)).isTrue();
		assertThatThrownBy(
				() -> TeamscalePropertiesUtils.parseCredentials(teamscalePropertiesPath)).hasMessageContaining("Failed to read");
	}

}