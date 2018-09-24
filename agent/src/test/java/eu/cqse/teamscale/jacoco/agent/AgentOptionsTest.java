package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.client.TeamscaleServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

import static eu.cqse.teamscale.client.EReportFormat.JUNIT;
import static eu.cqse.teamscale.client.EReportFormat.TESTWISE_COVERAGE;
import static eu.cqse.teamscale.client.EReportFormat.TEST_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests the {@link AgentOptions}. */
public class AgentOptionsTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	private String oldWorkingDir = System.getProperty("user.dir");

	@Before
	public void setUp() throws IOException {
		testFolder.create();
		testFolder.newFile("file_with_manifest1.jar");
		testFolder.newFolder("plugins");
		testFolder.newFolder("plugins", "inner");
		testFolder.newFile("plugins/some_other_file.jar");
		testFolder.newFile("plugins/file_with_manifest2.jar");
	}

	@After
	public void tearDown() {
		System.setProperty("user.dir", oldWorkingDir);
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

	/** Tests the options for uploading coverage to teamscale. */
	@Test
	public void testTeamscaleUploadOptions() throws AgentOptionParseException {
		new AgentOptionsParser();
		AgentOptions agentOptions = AgentOptionsParser.parse("out=.,class-dir=.," +
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
		AgentOptions agentOptions = AgentOptionsParser.parse("out=.,class-dir=.," +
				"http-server-port=8081," +
				"http-server-formats=TESTWISE_COVERAGE;TEST_LIST;JUNIT");
		assertThat(agentOptions.getHttpServerReportFormats())
				.containsExactlyInAnyOrder(TESTWISE_COVERAGE, TEST_LIST, JUNIT);
		assertThat(agentOptions.getHttpServerPort()).isEqualTo(8081);
	}

	/** Returns the include filter predicate for the given filter expression. */
	private static Predicate<String> includeFilter(String filterString) throws AgentOptionParseException {
		AgentOptions agentOptions = AgentOptionsParser.parse("out=.,class-dir=.,includes=" + filterString);
		return string -> agentOptions.getLocationIncludeFilter().test(string);
	}

	/** Returns the include filter predicate for the given filter expression. */
	private static Predicate<String> excludeFilter(String filterString) throws AgentOptionParseException {
		AgentOptions agentOptions = AgentOptionsParser.parse("out=.,class-dir=.,excludes=" + filterString);
		return string -> agentOptions.getLocationIncludeFilter().test(string);
	}

	/** Tests path resolution with and without patterns and with relative and absolute paths. */
	@Test
	public void testPathResolution() throws AgentOptionParseException, IOException {
		// Test with absolute path
		assertMatches(".", testFolder.getRoot().getAbsolutePath(), "");

		// Test with relative path
		assertMatches(".", ".", "");
		assertMatches("plugins", "../file_with_manifest1.jar", "file_with_manifest1.jar");

		// Test with pattern
		assertMatches(".", "plugins/file_*.jar", "plugins/file_with_manifest2.jar");
		assertMatches(".", "*/file_*.jar", "plugins/file_with_manifest2.jar");
		assertMatches("plugins/inner", "..", "plugins");
		assertMatches("plugins/inner", "../s*", "plugins/some_other_file.jar");

		assertFails(".", "**.jar", "Multiple files match the given pattern! " +
				"Only one match is allowed. Candidates are: [plugins/some_other_file.jar, " +
				"plugins/file_with_manifest2.jar, file_with_manifest1.jar]");

		assertFails(".", "**.war", "Invalid path given for option option-name: " +
				"**.war! The pattern **.war did not match any files in");
	}

	private void assertMatches(String workingDir, String input, String expected) throws AgentOptionParseException, IOException {
		setWorkingDirectory(new File(testFolder.getRoot(), workingDir));
		Path actualPath = AgentOptionsParser.parsePath("option-name", input);
		Path expectedPath = new File(testFolder.getRoot(), expected).toPath();
		assertThat(getCanonicalPath(actualPath)).isEqualByComparingTo(getCanonicalPath(expectedPath));
	}

	/** Resolves the path to its canonical path. */
	private static Path getCanonicalPath(Path path) throws IOException {
		return path.toFile().getCanonicalFile().toPath();
	}

	private void assertFails(String workingDir, String input, String expectedMessage) {
		setWorkingDirectory(new File(testFolder.getRoot(), workingDir));
		assertThatThrownBy(() -> AgentOptionsParser.parsePath("option-name", input))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(expectedMessage);
	}

	private static void setWorkingDirectory(File workingDirectory) {
		System.setProperty("user.dir", workingDirectory.getAbsolutePath());
	}
}
