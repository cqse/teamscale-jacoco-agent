package com.teamscale.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures that the agent logs its version on startup at level INFO.
 */
public class LogVersionOnStartupTest {

	private static final Path LOG_DIRECTORY = Paths.get("logTest").resolve("logs");
	private static final String AGENT_VERSION = System.getenv("AGENT_VERSION");

	@Test
	public void systemTest() throws Exception {
		assertThat(Files.exists(LOG_DIRECTORY)).isTrue();
		String logContent = String.join("\n", Files.readAllLines(LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log")));
		assertThat(logContent).containsPattern("INFO.*" + Pattern.quote(AGENT_VERSION));
	}
}
