package com.teamscale.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DoubleStartAgentTest {

	private static final Path LOG_DIRECTORY = Paths.get("logTest").resolve("logs");

	@Test
	public void systemTest() throws IOException {
		assertThat(LOG_DIRECTORY).exists();
		List<String> lines = Files.readAllLines(LOG_DIRECTORY.resolve("teamscale-jacoco-agent.log"));
		Stream<String> agentStartLines = lines.stream().filter(line -> line.contains("Starting JaCoCo agent"));
		assertThat(agentStartLines).hasSize(1);
	}
}
