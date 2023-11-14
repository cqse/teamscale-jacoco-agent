package com.teamscale.tia;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.io.ProcessUtils;
import org.conqat.lib.commons.system.SystemUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests the automatic conversion of .exec files to a testwise coverage report.
 */
public class TiaMavenCoverageConverterTest {
	/**
	 * Starts a maven process with the reuseForks flag set to "false" and tiaMode "exec-file".
	 * Checks if the coverage can be converted to a testwise coverage report afterward.
	 */
	@Test
	public void testMavenTia() throws Exception {
		File workingDirectory = new File("maven-exec-file-project");
		String executable = "./mvnw";
		if (SystemUtils.isWindows()) {
			executable = Paths.get("maven-exec-file-project", "mvnw.cmd").toUri().getPath();
		}

		ProcessUtils.ExecutionResult result = ProcessUtils.execute(new ProcessBuilder(executable, "clean", "verify").directory(workingDirectory));
		File configFile = new File(Paths.get(workingDirectory.getAbsolutePath(), "target", "tia", "agent.log").toUri());
		String configContent = FileSystemUtils.readFile(configFile);
		assertThat(configContent).isNotEmpty();
		assertThat(result.terminatedByTimeoutOrInterruption()).isFalse();
		assertThat(configContent).doesNotContain("Could not start http server on port");
		File testwiseCoverage = new File(Paths.get(workingDirectory.getAbsolutePath(), "target", "tia", "testwise-coverage-1.json").toUri());
		assertThat(testwiseCoverage).isNotEmpty();
	}
}
