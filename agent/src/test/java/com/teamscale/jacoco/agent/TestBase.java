package com.teamscale.jacoco.agent;

import com.teamscale.jacoco.agent.util.AgentUtils;
import org.junit.jupiter.api.AfterAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class that should be used for all tests.
 * Removes coverage directories created by the tests.
 */
public class TestBase {

	/**
	 * Remove all coverage directories created by the tests
	 */
	@AfterAll
	static void teardown() throws IOException {
		Path coverageDir = AgentUtils.getAgentDirectory().resolve("coverage");
		Files.list(coverageDir).forEach(path -> path.toFile().delete());
	}
}
