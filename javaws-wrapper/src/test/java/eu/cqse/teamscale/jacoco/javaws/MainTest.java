package eu.cqse.teamscale.jacoco.javaws;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MainTest {

	@Rule
	public TemporaryFolder folderRule = new TemporaryFolder();

	@Test
	public void test() throws Exception {
		runWrapper(0);
	}

	private Result runWrapper(int exitCode) throws IOException {
		Path testAppPath = Paths.get("test-data", getClass().getPackage().getName(), "test-app.jar").toAbsolutePath();
		System.err.println(testAppPath);

		File propertiesFile = folderRule.newFile(Main.PROPERTIES_FILENAME);
		List<String> lines = Arrays.asList(Main.PROPERTY_ARGENT_ARGUMENTS + "=exitCode=" + exitCode,
				Main.PROPERTY_JAVAWS + "=" + testAppPath);
		Files.write(propertiesFile.toPath(), lines, StandardOpenOption.CREATE);
		// TODO (FS)
		return null;
	}

	private static class Result {

		private final int exitCode;
		private final String stdOut;
		private final String stdIn;

		private Result(int exitCode, String stdOut, String stdIn) {
			this.exitCode = exitCode;
			this.stdOut = stdOut;
			this.stdIn = stdIn;
		}

	}

}
