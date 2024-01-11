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
		Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
		try (Stream<Path> stream = Files.list(tempDirectory)) {
			Optional<Path> maybeTempDirectory = stream.
					filter(path -> path.getFileName().toString().contains("teamscale-java-profiler"))
					.sorted().findFirst();
			return maybeTempDirectory.orElseThrow(() -> new AssertionError("Could not locate agent temp directory"));
		}
	}

}
