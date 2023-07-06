package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.util.TestUtils;
import com.teamscale.report.util.CommandLineLogger;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests parsing of the agent's command line options. */
public class AgentOptionsParserTest {

	private final AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), null);

	@Test
	public void environmentConfigOverridesCommandLineOptions(@TempDir Path tempDir) throws IOException, AgentOptionParseException {
		Path config = tempDir.resolve("config.properties");
		Files.write(config, "debug=true".getBytes(StandardCharsets.UTF_8));
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), config.toString());
		AgentOptions options = parser.parse("debug=false");

		assertThat(options.debugLogging).isEqualTo(true);
	}

	@Test
	public void environmentConfigPathDoesNotExist() {
		assertThatThrownBy(() -> {
			new AgentOptionsParser(new CommandLineLogger(), "/this/file/doesnt/exist").parse("");
		}).hasMessageContaining("not found");
	}

	@Test
	public void notGivingAnyOptionsShouldBeOK() throws Exception {
		parser.parse("");
		parser.parse(null);
	}

	@Test
	public void mustPreserveDefaultExcludes() throws Exception {
		assertThat(parser.parse("").jacocoExcludes).isEqualTo(AgentOptions.DEFAULT_EXCLUDES);
		assertThat(parser.parse("excludes=**foo**").jacocoExcludes)
				.isEqualTo("**foo**:" + AgentOptions.DEFAULT_EXCLUDES);
	}

	@Test
	public void mustDoHttpsRewriteForNoSchemePort443() throws Exception {
		assertThat(parser.parse(
				"upload-url=teamscale.url.com:443").uploadUrl)
				.isEqualTo(HttpUrl.parse("https://teamscale.url.com:443/"));
	}

	@Test
	public void mustNotDoHttpsRewriteForSchemePort443() throws Exception {
		assertThat(parser.parse(
				"upload-url=http://teamscale.url.com:443").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:443/"));
	}

	@Test
	public void defaultHttpRewriteForNoScheme() throws Exception {
		assertThat(parser.parse(
				"upload-url=teamscale.url.com:8080").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:8080/"));
		assertThat(parser.parse(
				"upload-url=teamscale.url.com:80").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:80/"));
		assertThat(parser.parse(
				"upload-url=teamscale.url.com:444").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:444/"));
	}

	/**
	 * Delete created coverage folders
	 */
	@AfterAll
	public static void teardown() throws IOException {
		TestUtils.cleanAgentCoverageDirectory();
	}
}
