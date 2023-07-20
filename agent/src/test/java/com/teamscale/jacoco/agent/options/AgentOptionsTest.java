package com.teamscale.jacoco.agent.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryConfig;
import com.teamscale.jacoco.agent.util.TestUtils;
import com.teamscale.report.util.CommandLineLogger;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		assertThat(agentOptions.getDumpIntervalInMinutes()).isEqualTo(480);
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
		assertThat(teamscaleServer.getMessage()).isEqualTo("This is my message");
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
				"http-server-port=8081,tia-mode=http");
		assertThat(agentOptions.getHttpServerPort()).isEqualTo(8081);
		assertThat(agentOptions.getTestwiseCoverageMode()).isEqualTo(ETestwiseCoverageMode.HTTP);
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

	/** Tests default for ignore-uncovered-classes is false. */
	@Test
	public void shouldAllowMinusForEnumConstants() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("tia-mode=exec-file");
		assertThat(agentOptions.getTestwiseCoverageMode()).isEqualTo(ETestwiseCoverageMode.EXEC_FILE);
	}

	/** Tests that supplying both revision and commit info is forbidden. */
	@Test
	public void testBothRevisionAndCommitSupplied() throws URISyntaxException {
		String message = "'teamscale-revision' and 'teamscale-revision-manifest-jar' are incompatible with "
				+ "'teamscale-commit' and 'teamscale-commit-manifest-jar'.";

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
						"teamscale-revision-manifest-jar=" + jar.getAbsolutePath() + ",teamscale-commit=master:1000"))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(message);
		assertThatThrownBy(
				() -> getAgentOptionsParserWithDummyLogger().parse(
						"teamscale-revision-manifest-jar=" + jar.getAbsolutePath() + ",teamscale-commit-manifest-jar=" + jar.getAbsolutePath()))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(message);
	}

	/** Tests the 'teamscale-revision-manifest-jar' option correctly parses the 'Git_Commit' field in the manifest. */
	@Test
	public void testTeamscaleRevisionManifestJarOption() throws URISyntaxException, AgentOptionParseException {
		File jar = new File(getClass().getResource("manifest-with-git-commit-revision.jar").toURI());
		AgentOptions options = getAgentOptionsParserWithDummyLogger().parse(
				"teamscale-revision-manifest-jar=" + jar.getAbsolutePath());

		assertThat(options.getTeamscaleServerOptions().revision).isEqualTo("f364d58dc4966ca856260185e46a90f80ee5e9c6");
	}

	/**
	 * Tests that an exception is thrown when the user attempts to specify the commit/revision when 'teamscale-project'
	 * is not specified. The absence of the `teamscale-project` implies a multi-project upload, so the commit/revision
	 * have to be provided individually via the 'git.properties' file.
	 */
	@Test
	public void testNoCommitOrRevisionGivenWhenProjectNull() {
		String message = "You tried to provide a commit to upload to directly." +
				" This is not possible, since you did not provide the 'teamscale-project' Teamscale project to upload to";

		assertThatThrownBy(
				() -> getAgentOptionsParserWithDummyLogger().parse(
						"teamscale-server-url=127.0.0.1," +
								"teamscale-user=build," +
								"teamscale-access-token=token," +
								"teamscale-partition=\"Unit Tests\"," +
								"teamscale-commit=default:HEAD," +
								"teamscale-message=\"This is my message\""))
				.isInstanceOf(AgentOptionParseException.class).hasMessageContaining(message);
		assertThatThrownBy(
				() -> getAgentOptionsParserWithDummyLogger().parse(
						"teamscale-server-url=127.0.0.1," +
								"teamscale-user=build," +
								"teamscale-access-token=token," +
								"teamscale-partition=\"Unit Tests\"," +
								"teamscale-revision=1234ABCD," +
								"teamscale-message=\"This is my message\""))
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

	/** Tests the options for SAP NWDI applications. */
	@Test
	public void testValidSapNwdiOptions() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("" +
				"teamscale-server-url=http://your.teamscale.url," +
				"teamscale-user=your-user-name," +
				"teamscale-access-token=your-access-token," +
				"teamscale-partition=Manual Tests," +
				"sap-nwdi-applications=com.example:project;com.test:p2");
		assertThat(agentOptions.sapNetWeaverJavaApplications)
				.isNotNull()
				.satisfies(sap -> {
					assertThat(sap.getApplications()).hasSize(2);
					assertThat(sap.getApplications()).element(0).satisfies(application -> {
						assertThat(application.markerClass).isEqualTo("com.example");
						assertThat(application.teamscaleProject).isEqualTo("project");
					});
					assertThat(sap.getApplications()).element(1).satisfies(application -> {
						assertThat(application.markerClass).isEqualTo("com.test");
						assertThat(application.teamscaleProject).isEqualTo("p2");
					});
				});
		assertThat(agentOptions.teamscaleServer.hasAllRequiredFieldsSetExceptProject()).isTrue();
		assertThat(agentOptions.teamscaleServer.hasAllRequiredFieldsSet()).isFalse();
	}

	/**
	 * Tests successful parsing of the {@link ArtifactoryConfig#ARTIFACTORY_API_KEY_OPTION}
	 */
	@Test
	public void testArtifactoryApiKeyOptionIsCorrectlyParsed() throws AgentOptionParseException {
		String someArtifactoryApiKey = "some_api_key";
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse(
				String.format("%s=%s,%s=%s,%s=%s", ArtifactoryConfig.ARTIFACTORY_URL_OPTION, "http://some_url",
						ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION, someArtifactoryApiKey,
						ArtifactoryConfig.ARTIFACTORY_PARTITION, "partition"));
		assertThat(agentOptions.artifactoryConfig.apiKey).isEqualTo(someArtifactoryApiKey);
	}

	/**
	 * Tests that setting {@link ArtifactoryConfig#ARTIFACTORY_USER_OPTION} and {@link
	 * ArtifactoryConfig#ARTIFACTORY_PASSWORD_OPTION} (along with {@link ArtifactoryConfig#ARTIFACTORY_URL_OPTION})
	 * passes the AgentOptions' validity check.
	 */
	@Test
	public void testArtifactoryBasicAuthSetPassesValiditiyCheck() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("");
		agentOptions.artifactoryConfig.url = HttpUrl.get("http://some_url");
		agentOptions.artifactoryConfig.user = "user";
		agentOptions.artifactoryConfig.password = "password";
		agentOptions.artifactoryConfig.partition = "partition";
		assertThat(agentOptions.getValidator().isValid()).isTrue();
	}

	/**
	 * Tests that setting {@link ArtifactoryConfig#ARTIFACTORY_USER_OPTION} and {@link
	 * ArtifactoryConfig#ARTIFACTORY_API_KEY_OPTION} (along with {@link ArtifactoryConfig#ARTIFACTORY_URL_OPTION})
	 * passes the AgentOptions' validity check.
	 */
	@Test
	public void testArtifactoryApiKeySetPassesValidityCheck() throws AgentOptionParseException {
		AgentOptions agentOptions = getAgentOptionsParserWithDummyLogger().parse("");
		agentOptions.artifactoryConfig.url = HttpUrl.get("http://some_url");
		agentOptions.artifactoryConfig.apiKey = "api_key";
		agentOptions.artifactoryConfig.partition = "partition";
		assertThat(agentOptions.getValidator().isValid()).isTrue();
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

	private static AgentOptionsParser getAgentOptionsParserWithDummyLogger() {
		return new AgentOptionsParser(new CommandLineLogger(), null, null);
	}

	/**
	 * Delete created coverage folders
	 */
	@AfterAll
	public static void teardown() throws IOException {
		TestUtils.cleanAgentCoverageDirectory();
	}
}
