package com.teamscale.jacoco.agent.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test Utilities
 */
public class TestUtils {
	/**
	 * Deletes all contents inside the coverage folder inside the agent directory
	 */
	public static void cleanAgentCoverageDirectory() throws IOException {
		Path coverageDir = AgentUtils.getAgentDirectory().resolve("coverage");
		if (Files.exists(coverageDir)) {
			try (Stream<Path> stream = Files.list(coverageDir)) {
				stream.forEach(path ->
						assertThat(path.toFile().delete()).withFailMessage("Failed to delete " + path).isTrue());
			}
			Files.delete(coverageDir);
		}
	}

	/** Returns a new free TCP port number */
	public static int getFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		}
	}
}
