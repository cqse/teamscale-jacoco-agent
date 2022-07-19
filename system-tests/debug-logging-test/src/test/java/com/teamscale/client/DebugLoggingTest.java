package com.teamscale.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class DebugLoggingTest {

	private static final Path LOG_DIRECTORY = Paths.get("logTest").resolve("logs");

	@Test
	public void systemTest() throws Exception {
		assertThat(Files.exists(LOG_DIRECTORY)).isTrue();
		assertThat(Files.readAllLines(LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log"))).isNotEmpty();
	}
}
