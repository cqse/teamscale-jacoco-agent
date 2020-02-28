package com.teamscale.jacoco.agent.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test Utilities
 */
public class TestUtils {
	/**
	 * Deletes all contents inside the coverage folder inside the agent directory
	 */
	public static void cleanAgentCoverageDirectory() throws IOException {
		Path coverageDir = AgentUtils.getAgentDirectory().resolve("coverage");
		Files.list(coverageDir).forEach(path -> path.toFile().delete());
	}
}
