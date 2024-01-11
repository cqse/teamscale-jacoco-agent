package com.teamscale.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleAgentsTest {

	private static final Path LOG_DIRECTORY = Paths.get("logTest").resolve("logs");

	@Test
	public void systemTest() throws IOException {
		assertThat(LOG_DIRECTORY).exists();
		List<String> lines = Files.readAllLines(LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log"));
		assertThat(lines).anyMatch(
				s -> s.contains("Using multiple java agents could interfere with coverage recording.")
		);
		assertThat(lines).anyMatch(
				s -> s.contains("For best results consider registering the Teamscale JaCoCo Agent first.")
		);
	}
}
