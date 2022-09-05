package com.teamscale.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class DebugLoggingDefaultTest {

	private static final Path LOG_DIRECTORY = Paths.get("../../agent/build", "logs");

	@Test
	public void systemTest() throws Exception {
		assertThat(Files.exists(LOG_DIRECTORY)).isTrue();
		assertThat(Files.readAllLines(LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log"))).isNotEmpty();
	}
}
