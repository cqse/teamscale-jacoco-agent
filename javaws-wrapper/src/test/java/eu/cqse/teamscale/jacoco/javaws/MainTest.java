package eu.cqse.teamscale.jacoco.javaws;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;

import eu.cqse.teamscale.jacoco.javaws.Main.ConfigurationException;

public class MainTest {

	@Rule
	public TemporaryFolder folderRule = new TemporaryFolder();

	@Rule
	public SystemOutRule systemOutRule = new SystemOutRule().enableLog();

	@Rule
	public SystemErrRule systemErrRule = new SystemErrRule().enableLog();

	private int lastExitCode;

	@Before
	public void reset() {
		lastExitCode = -1;
	}

	@Test
	public void test() throws Exception {
		Result result = runWrapper(0, "argument1", "argument2");

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(result.exitCode).as("exit code").isEqualTo(0);
		softly.assertThat(result.stdOut).as("stdout").isEqualTo("argument1\nargument2\n");
		softly.assertThat(result.stdErr).as("stderr").isEqualTo("error");
		softly.assertAll();
	}

	private Result runWrapper(int expectedExitCode, String... arguments)
			throws IOException, InterruptedException, ConfigurationException {
		Path testAppPath = Paths.get("test-data", getClass().getPackage().getName(), "bin/test-app").toAbsolutePath();

		File propertiesFile = folderRule.newFile(Main.PROPERTIES_FILENAME);
		List<String> lines = Arrays.asList(Main.PROPERTY_ARGENT_ARGUMENTS + "=exitCode=" + expectedExitCode,
				Main.PROPERTY_JAVAWS + "=" + testAppPath);
		Files.write(propertiesFile.toPath(), lines, StandardOpenOption.CREATE);

		new Main().run(exitCode -> lastExitCode = exitCode, arguments, folderRule.getRoot().toPath().toAbsolutePath());

		String stdOut = systemOutRule.getLogWithNormalizedLineSeparator();
		String stdErr = systemErrRule.getLogWithNormalizedLineSeparator();
		return new Result(lastExitCode, stdOut, stdErr);
	}

	private static class Result {

		private final int exitCode;
		private final String stdOut;
		private final String stdErr;

		private Result(int exitCode, String stdOut, String stdErr) {
			this.exitCode = exitCode;
			this.stdOut = stdOut;
			this.stdErr = stdErr;
		}

	}

}
