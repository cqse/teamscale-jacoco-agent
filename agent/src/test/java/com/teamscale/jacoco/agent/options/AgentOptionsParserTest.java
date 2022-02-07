package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.util.TestUtils;
import com.teamscale.report.util.CommandLineLogger;
import okhttp3.HttpUrl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/** Tests parsing of the agent's command line options. */
public class AgentOptionsParserTest {

	private final AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger());

	@Test
	public void notGivingAnyOptionsShouldBeOK() throws Exception {
		parser.parse("");
		parser.parse(null);
	}

	@Test
	public void mustPreserveDefaultExcludes() throws Exception {
		Assertions.assertThat(parser.parse("").jacocoExcludes).isEqualTo(AgentOptions.DEFAULT_EXCLUDES);
		Assertions.assertThat(parser.parse("excludes=**foo**").jacocoExcludes)
				.isEqualTo("**foo**:" + AgentOptions.DEFAULT_EXCLUDES);
	}

	@Test
	public void mustDoHttpsRewriteForNoSchemePort443() throws Exception {
		Assertions.assertThat(parser.parse(
						"upload-url=teamscale.url.com:443").uploadUrl)
				.isEqualTo(HttpUrl.parse("https://teamscale.url.com:443/"));
	}

	@Test
	public void mustNotDoHttpsRewriteForSchemePort443() throws Exception {
		Assertions.assertThat(parser.parse(
						"upload-url=http://teamscale.url.com:443").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:443/"));
	}

	@Test
	public void defaultHttpRewriteForNoScheme() throws Exception {
		Assertions.assertThat(parser.parse(
						"upload-url=teamscale.url.com:8080").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:8080/"));
		Assertions.assertThat(parser.parse(
						"upload-url=teamscale.url.com:80").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:80/"));
		Assertions.assertThat(parser.parse(
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
