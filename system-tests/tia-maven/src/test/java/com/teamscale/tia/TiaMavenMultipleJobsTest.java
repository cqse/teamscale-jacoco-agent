package com.teamscale.tia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Paths;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.system.SystemUtils;
import org.junit.jupiter.api.Test;

/**
 * Test class to check if multiple maven plugins can be started with dynamic
 * port allocation.
 */
public class TiaMavenMultipleJobsTest {

	/**
	 * Starts multiple maven processes and checks that the ports are dynamically set
	 * and the servers are correctly started.
	 */
	@Test
	public void testMavenTia() throws Exception {
		File workingDirectory = new File("maven-dump-local-project");
		ProcessBuilder[] processes = new ProcessBuilder[4];
		String executable = "./mvnw";
		if (SystemUtils.isWindows()) {
			executable = Paths.get("maven-dump-local-project", "mvnw.cmd").toUri().getPath();
		}
		for (int i = 0; i < processes.length; i++) {
			new ProcessBuilder(executable, "clean", "verify").directory(workingDirectory).start();
		}
		// Wait for processes to do something.
		Thread.sleep(10000);
		File configFile = new File(Paths.get("maven-dump-local-project", "target", "tia", "agent.log").toUri());
		String configContent = FileSystemUtils.readFile(configFile);
		assertThat(configContent).isNotEmpty();
		assertThat(configContent).doesNotContain("Could not start http server on port");
	}
}
