package com.teamscale.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleJavaAgentsTest {

	private static Path LOG_DIRECTORY = Paths.get("").resolve("logs");

	@Test
	public void systemTest() throws Exception {
		assertThat(Files.exists(LOG_DIRECTORY)).isTrue();
		List<String> lines = Files.readAllLines(LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log"));
		assertThat(lines).contains("Using multiple java agents could interfere with coverage recording.");
		assertThat(lines).contains("For best results consider registering the Teamscale JaCoCo Agent first.");
	}
}
