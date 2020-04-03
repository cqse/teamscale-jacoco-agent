package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.util.TestUtils;
import com.teamscale.report.util.CommandLineLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests the {@link AgentOptions}. */
public class FilePatternResolverTest {

	@TempDir
	public File testFolder;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@BeforeEach
	public void setUp() throws IOException {
		new File(testFolder, "file_with_manifest1.jar").createNewFile();
		new File(testFolder, "plugins/inner").mkdirs();
		new File(testFolder, "plugins/some_other_file.jar").createNewFile();
		new File(testFolder, "plugins/file_with_manifest2.jar").createNewFile();
	}

	/** Tests path resolution with absolute path. */
	@Test
	public void testPathResolutionForAbsolutePath() throws AgentOptionParseException {
		assertInputInWorkingDirectoryMatches(".", testFolder.getAbsolutePath(), "");
	}

	/** Tests path resolution with relative paths. */
	@Test
	public void testPathResolutionForRelativePath() throws AgentOptionParseException {
		assertInputInWorkingDirectoryMatches(".", ".", "");
		assertInputInWorkingDirectoryMatches("plugins", "../file_with_manifest1.jar", "file_with_manifest1.jar");
	}

	/** Tests path resolution with patterns and relative paths. */
	@Test
	public void testPathResolutionWithPatternsAndRelativePaths() throws AgentOptionParseException {
		assertInputInWorkingDirectoryMatches(".", "plugins/file_*.jar", "plugins/file_with_manifest2.jar");
		assertInputInWorkingDirectoryMatches(".", "*/file_*.jar", "plugins/file_with_manifest2.jar");
		assertInputInWorkingDirectoryMatches("plugins/inner", "..", "plugins");
		assertInputInWorkingDirectoryMatches("plugins/inner", "../s*", "plugins/some_other_file.jar");
	}

	/** Tests path resolution with patterns and absolute paths. */
	@Test
	public void testPathResolutionWithPatternsAndAbsolutePaths() throws AgentOptionParseException {
		assertInputInWorkingDirectoryMatches("plugins", testFolder.getAbsolutePath() + "/plugins/file_*.jar",
				"plugins/file_with_manifest2.jar");
	}

	private void assertInputInWorkingDirectoryMatches(String workingDir, String input,
													  String expected) throws AgentOptionParseException {
		final File workingDirectory = new File(testFolder, workingDir);
		File actualFile = getFilePatternResolverWithDummyLogger().parsePath("option-name", workingDirectory, input)
				.toFile();
		File expectedFile = new File(testFolder, expected);
		assertThat(getNormalizedPath(actualFile)).isEqualByComparingTo(getNormalizedPath(expectedFile));
	}

	/** Resolves the path to its absolute normalized path. */
	private static Path getNormalizedPath(File file) {
		return file.getAbsoluteFile().toPath().normalize();
	}

	/** Tests path resolution with incorrect input. */
	@Test
	public void testPathResolutionWithPatternErrorCases() {
		final File workingDirectory = testFolder;
		assertThatThrownBy(
				() -> getFilePatternResolverWithDummyLogger().parsePath("option-name", workingDirectory, "**.war"))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(
				"Invalid path given for option option-name: " +
						"**.war. The pattern **.war did not match any files in");
	}

	private static FilePatternResolver getFilePatternResolverWithDummyLogger() {
		return new FilePatternResolver(new CommandLineLogger());
	}

	/**
	 * Delete created coverage folders
	 */
	@AfterAll
	public static void teardown() throws IOException {
		TestUtils.cleanAgentCoverageDirectory();
	}
}
