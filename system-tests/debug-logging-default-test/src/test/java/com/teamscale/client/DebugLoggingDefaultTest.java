package com.teamscale.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DebugLoggingDefaultTest {

	@Test
	public void systemTest() throws Exception {
		Path logDirectory = searchForAgentTempDirectory().resolve("logs");
		assertThat(logDirectory).exists();
		assertThat(Files.readAllLines(logDirectory.resolve("teamscale-jacoco-agent.log"))).isNotEmpty();
	}

	private Path searchForAgentTempDirectory() throws IOException {
		try (Stream<Path> stream = findAllAgentTempDirectories()) {
			Optional<Path> maybeTempDirectory = stream.sorted().findFirst();
			return maybeTempDirectory.orElseThrow(() -> new AssertionError("Could not locate agent temp directory"));
		}
	}

	private Stream<Path> findAllAgentTempDirectories() throws IOException {
		Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
		return Files.list(tempDirectory)
				.filter(path -> path.getFileName().toString().contains("teamscale-java-profiler"));
	}
}
