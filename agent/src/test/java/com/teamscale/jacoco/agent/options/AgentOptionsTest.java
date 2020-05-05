package com.teamscale.jacoco.agent.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.util.TestUtils;
import com.teamscale.report.util.CommandLineLogger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests the {@link AgentOptions}. */
public class AgentOptionsTest {

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

	/** Tests include pattern matching. */
	@Test
	public void testIncludePatternMatching() throws AgentOptionParseException {
		assertThat(includeFilter("com.*")).accepts("file.jar@com/foo/Bar.class", "file.jar@com/foo/Bar$Goo.class",
				"file1.jar@goo/file2.jar@com/foo/Bar.class", "com/foo/Bar.class", "com.foo/Bar.class");
		assertThat(includeFilter("com.*")).rejects("foo/com/Bar.class", "com.class", "file.jar@com.class",
				"A$com$Bar.class");
		assertThat(includeFilter("*com.*")).accepts("file.jar@com/foo/Bar.class", "file.jar@com/foo/Bar$Goo.class",
				"file1.jar@goo/file2.jar@com/foo/Bar.class", "com/foo/Bar.class", "foo/com/goo/Bar.class",
				"A$com$Bar.class", "src/com/foo/Bar.class");
		assertThat(includeFilter("*com.*;*de.*"))
				.accepts("file.jar@com/foo/Bar.class", "file.jar@de/foo/Bar$Goo.class");
		assertThat(excludeFilter("*com.*;*de.*"))
				.rejects("file.jar@com/foo/Bar.class", "file.jar@de/foo/Bar$Goo.class");
		assertThat(includeFilter("*com.customer.*")).accepts(
				"C:\\client-daily\\client\\plugins\\com.customer.something.client_1.2.3.4.1234566778.jar@com/customer/something/SomeClass.class");
	}

	/** Interval options test. */
	@Test
	public void testIntervalOptions() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("");
		assertThat(agentOptions.getDumpIntervalInMinutes()).isEqualTo(60);
		agentOptions = getAgentOptionsParserWithDummyLogger().parse("interval=0");
		assertThat(agentOptions.shouldDumpInIntervals()).isEqualTo(false);
		agentOptions = getAgentOptionsParserWithDummyLogger().parse("interval=30");
		assertThat(agentOptions.shouldDumpInIntervals()).isEqualTo(true);
		assertThat(agentOptions.getDumpIntervalInMinutes()).isEqualTo(30);
	}

	/** Tests the options for uploading coverage to teamscale. */
	@Test
	public void testTeamscaleUploadOptions() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("" +
				"teamscale-server-url=127.0.0.1," +
				"teamscale-project=test," +
				"teamscale-user=build," +
				"teamscale-access-token=token," +
				"teamscale-partition=\"Unit Tests\"," +
				"teamscale-commit=default:HEAD," +
				"teamscale-message=\"This is my message\"");

		TeamscaleServer teamscaleServer = agentOptions.getTeamscaleServerOptions();
		assertThat(teamscaleServer.url.toString()).isEqualTo("http://127.0.0.1/");
		assertThat(teamscaleServer.project).isEqualTo("test");
		assertThat(teamscaleServer.userName).isEqualTo("build");
		assertThat(teamscaleServer.userAccessToken).isEqualTo("token");
		assertThat(teamscaleServer.partition).isEqualTo("Unit Tests");
		assertThat(teamscaleServer.commit.toString()).isEqualTo("default:HEAD");
		assertThat(teamscaleServer.message).isEqualTo("This is my message");
	}

	/** Tests the options for the Test Impact mode. */
	@Test
	public void testHttpServerOptions() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("mode=TESTWISE,class-dir=.," +
				"http-server-port=8081");
		assertThat(agentOptions.getHttpServerPort()).isEqualTo(8081);
	}

	/** Tests the options http-server-port option for normal mode. */
	@Test
	public void testHttpServerOptionsForNormalMode() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("http-server-port=8081");
		assertThat(agentOptions.getHttpServerPort()).isEqualTo(8081);
	}

	/** Tests the options for the Test Impact mode. */
	@Test
	public void testEnvironmentVariableOptions() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("mode=TESTWISE,class-dir=.," +
				"test-env=TEST");
		assertThat(agentOptions.getTestEnvironmentVariableName()).isEqualTo("TEST");
	}

	/** Tests the options for the Test Impact mode. */
	@Test
	public void testHttpServerOptionsWithCoverageViaHttp() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("mode=TESTWISE,class-dir=.," +
				"http-server-port=8081,coverage-via-http=true");
		assertThat(agentOptions.getHttpServerPort()).isEqualTo(8081);
		assertThat(agentOptions.getTestWiseCoverageMode()).isEqualTo(ETestWiseCoverageMode.HTTP);
	}
	
	/** Tests setting ignore-uncovered-classes works. */
	@Test
	public void testIgnoreUncoveredClasses() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("ignore-uncovered-classes=true");
		assertTrue(agentOptions.shouldIgnoreUncoveredClasses());
	}

	/** Tests default for ignore-uncovered-classes is false. */
	@Test
	public void testIgnoreUncoveredClassesDefault() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("");
		assertFalse(agentOptions.shouldIgnoreUncoveredClasses());
	}
	
	/** Tests that supplying both revision and commit info is forbidden. */
	@Test
	public void testBothRevisionAndCommitSupplied() throws URISyntaxException {
		String message = "'teamscale-revision' is incompatible with 'teamscale-commit', "
				+ "'teamscale-commit-manifest-jar', or 'teamscale-git-properties-jar'.";

		File jar = new File(getClass().getResource("manifest-and-git-properties.jar").toURI());

		assertThatThrownBy(
				() -> getAgentOptionsParserWithDummyLogger().parse(
						"teamscale-revision=1234,teamscale-commit=master:1000"))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(message);
		assertThatThrownBy(
				() -> getAgentOptionsParserWithDummyLogger().parse(
						"teamscale-revision=1234,teamscale-commit-manifest-jar=" + jar.getAbsolutePath()))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(message);
		assertThatThrownBy(
				() -> getAgentOptionsParserWithDummyLogger().parse(
						"teamscale-revision=1234,teamscale-git-properties-jar=" + jar.getAbsolutePath()))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(message);
	}

	/** Tests that supplying version info is supported in Testwise mode. */
	@Test
	public void testVersionInfosInTestwiseMode() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("mode=TESTWISE,class-dir=.," +
				"http-server-port=8081,teamscale-revision=1234");
		assertThat(agentOptions.getTeamscaleServerOptions().revision).isEqualTo("1234");

		agentOptions = getAgentOptionsParserWithDummyLogger().parse("mode=TESTWISE,class-dir=.," +
				"http-server-port=8081,teamscale-commit=branch:1234");
		assertThat(agentOptions.getTeamscaleServerOptions().commit).isEqualTo(CommitDescriptor.parse("branch:1234"));
	}

	/** Tests the options for azure file storage upload. */
	@Test
	public void testAzureFileStorageOptions() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("" +
				"azure-url=https://mrteamscaleshdev.file.core.windows.net/tstestshare/," +
				"azure-key=Ut0BQ2OEvgQXGnNJEjxnaEULAYgBpAK9+HukeKSzAB4CreIQkl2hikIbgNe4i+sL0uAbpTrFeFjOzh3bAtMMVg==");
		assertThat(agentOptions.azureFileStorageConfig.url.toString())
				.isEqualTo("https://mrteamscaleshdev.file.core.windows.net/tstestshare/");
		assertThat(agentOptions.azureFileStorageConfig.accessKey).isEqualTo(
				"Ut0BQ2OEvgQXGnNJEjxnaEULAYgBpAK9+HukeKSzAB4CreIQkl2hikIbgNe4i+sL0uAbpTrFeFjOzh3bAtMMVg==");
	}

	/** Returns the include filter predicate for the given filter expression. */
	private static Predicate<String> includeFilter(String filterString) throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger()
				.parse("includes=" + filterString);
		return string -> agentOptions.getLocationIncludeFilter().isIncluded(string);
	}

	/** Returns the include filter predicate for the given filter expression. */
	private static Predicate<String> excludeFilter(String filterString) throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger()
				.parse("excludes=" + filterString);
		return string -> agentOptions.getLocationIncludeFilter().isIncluded(string);
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

	/** Tests path resolution with incorrect input. */
	@Test
	public void testPathResolutionWithPatternErrorCases() {
		final File workingDirectory = testFolder;
		assertThatThrownBy(
				() -> getAgentOptionsParserWithDummyLogger().parsePath("option-name", workingDirectory, "**.war"))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(
				"Invalid path given for option option-name: " +
						"**.war. The pattern **.war did not match any files in");
	}

	private void assertInputInWorkingDirectoryMatches(String workingDir, String input,
													  String expected) throws AgentOptionParseException {
		final File workingDirectory = new File(testFolder, workingDir);
		File actualFile = getAgentOptionsParserWithDummyLogger().parsePath("option-name", workingDirectory, input)
				.toFile();
		File expectedFile = new File(testFolder, expected);
		assertThat(getNormalizedPath(actualFile)).isEqualByComparingTo(getNormalizedPath(expectedFile));
	}

	/** Resolves the path to its absolute normalized path. */
	private static Path getNormalizedPath(File file) {
		return file.getAbsoluteFile().toPath().normalize();
	}

	private static AgentOptionsParser getAgentOptionsParserWithDummyLogger() {
		return new AgentOptionsParser(new CommandLineLogger());
	}

	/**
	 * Delete created coverage folders
	 */
	@AfterAll
	public static void teardown() throws IOException {
		TestUtils.cleanAgentCoverageDirectory();
	}
}
