package com.teamscale.tia;

import com.teamscale.test.commons.SystemTestUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to check if multiple maven plugins can be started with dynamic port allocation.
 */
@Disabled("https://cqse.atlassian.net/browse/TS-38429")
public class TiaMavenMultipleJobsTest {

	/**
	 * Starts multiple Maven processes and checks that the ports are dynamically set and the servers are correctly
	 * started.
	 */
	@Test
	public void testMavenTia() throws Exception {
		String workingDirectory = "maven-dump-local-project";

		// Clean once before testing parallel execution and make sure that the cleaning
		// process is finished before testing.
		SystemTestUtils.runMaven(workingDirectory, "clean");

		// run three verify processes in parallel without waiting
		for (int i = 0; i < 3; i++) {
			SystemTestUtils.buildMavenProcess(workingDirectory, "verify").start();
		}

		// and one more that we wait for to terminate
		SystemTestUtils.runMaven(workingDirectory, "verify");

		Path configFile = Paths.get(workingDirectory, "target", "tia", "agent.log");
		String configContent = FileSystemUtils.readFile(configFile.toFile());
		assertThat(configContent).isNotEmpty().doesNotContain("Could not start http server on port");
	}
}
