package com.teamscale.jacoco.agent.options;

import com.teamscale.client.JsonUtils;
import com.teamscale.client.ProfilerConfiguration;
import com.teamscale.client.ProfilerRegistration;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryConfig;
import com.teamscale.jacoco.agent.util.TestUtils;
import com.teamscale.report.util.CommandLineLogger;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests parsing of the agent's command line options. */
public class AgentOptionsParserTest {

	private TeamscaleCredentials teamscaleCredentials;
	private Path configFile;
	/** The mock server to run requests against. */
	protected MockWebServer mockWebServer;

	/** Starts the mock server. */
	@BeforeEach
	public void setup() throws Exception {
		configFile = Paths.get(getClass().getResource("agent.properties").toURI());
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		teamscaleCredentials = new TeamscaleCredentials(mockWebServer.url("/"), "user", "key");
	}

	/** Shuts down the mock server. */
	@AfterEach
	public void cleanup() throws Exception {
		mockWebServer.shutdown();
	}

	@Test
	public void testUploadMethodRecognition() throws Exception {
		assertThat(parseAndThrow(null).determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.LOCAL_DISK);
		assertThat(parseAndThrow("azure-url=azure.com,azure-key=key").determineUploadMethod()).isEqualTo(
				AgentOptions.EUploadMethod.AZURE_FILE_STORAGE);
		assertThat(parseAndThrow(
				String.format("%s=%s,%s=%s,%s=%s", ArtifactoryConfig.ARTIFACTORY_URL_OPTION, "http://some_url",
						ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION, "apikey",
						ArtifactoryConfig.ARTIFACTORY_PARTITION, "partition")
		).determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.ARTIFACTORY);

		String basicTeamscaleOptions = "teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p";
		assertThat(parseAndThrow(basicTeamscaleOptions)
				.determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.TEAMSCALE_MULTI_PROJECT);
		assertThat(parseAndThrow(basicTeamscaleOptions + ",teamscale-project=proj")
				.determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.TEAMSCALE_SINGLE_PROJECT);
		assertThat(parseAndThrow(
						basicTeamscaleOptions + ",sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
				.determineUploadMethod())
				.isEqualTo(AgentOptions.EUploadMethod.SAP_NWDI_TEAMSCALE);
	}

	@Test
	public void testUploadMethodRecognitionWithTeamscaleProperties() throws Exception {
		TeamscaleCredentials credentials = new TeamscaleCredentials(HttpUrl.get("http://localhost"), "user", "key");
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), null, null, credentials);

		assertThat(parseAndThrow(null).determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.LOCAL_DISK);
		assertThat(parseAndThrow("azure-url=azure.com,azure-key=key").determineUploadMethod()).isEqualTo(
				AgentOptions.EUploadMethod.AZURE_FILE_STORAGE);
		assertThat(parseAndThrow(
				String.format("%s=%s,%s=%s,%s=%s", ArtifactoryConfig.ARTIFACTORY_URL_OPTION, "http://some_url",
						ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION, "apikey",
						ArtifactoryConfig.ARTIFACTORY_PARTITION, "partition")
		).determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.ARTIFACTORY);

		String basicTeamscaleOptions = "teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p";
		assertThat(parseAndThrow(basicTeamscaleOptions)
				.determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.TEAMSCALE_MULTI_PROJECT);
		assertThat(parseAndThrow(basicTeamscaleOptions + ",teamscale-project=proj")
				.determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.TEAMSCALE_SINGLE_PROJECT);
		assertThat(parseAndThrow(
						basicTeamscaleOptions + ",sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
				.determineUploadMethod())
				.isEqualTo(AgentOptions.EUploadMethod.SAP_NWDI_TEAMSCALE);
	}

	@Test
	public void environmentConfigIdOverridesCommandLineOptions() throws Exception {
		ProfilerRegistration registration = new ProfilerRegistration();
		registration.profilerId = UUID.randomUUID().toString();
		registration.profilerConfiguration = new ProfilerConfiguration();
		registration.profilerConfiguration.configurationId = "my-config";
		registration.profilerConfiguration.configurationOptions = "teamscale-partition=foo";
		mockWebServer.enqueue(new MockResponse().setBody(JsonUtils.serialize(registration)));
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), "my-config",
				null, teamscaleCredentials);
		AgentOptions options = parseAndThrow(parser, "teamscale-partition=bar");

		assertThat(options.teamscaleServer.partition).isEqualTo("foo");
	}

	@Test
	public void environmentConfigFileOverridesCommandLineOptions() throws Exception {
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), null, configFile.toString(),
				teamscaleCredentials);
		AgentOptions options = parseAndThrow(parser, "teamscale-partition=from-command-line");

		assertThat(options.teamscaleServer.partition).isEqualTo("from-config-file");
	}

	@Test
	public void environmentConfigFileOverridesConfigId() throws Exception {
		ProfilerRegistration registration = new ProfilerRegistration();
		registration.profilerId = UUID.randomUUID().toString();
		registration.profilerConfiguration = new ProfilerConfiguration();
		registration.profilerConfiguration.configurationId = "my-config";
		registration.profilerConfiguration.configurationOptions = "teamscale-partition=from-config-id";
		mockWebServer.enqueue(new MockResponse().setBody(JsonUtils.serialize(registration)));
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), "my-config", configFile.toString(),
				teamscaleCredentials);
		AgentOptions options = parseAndThrow(parser, "teamscale-partition=from-command-line");

		assertThat(options.teamscaleServer.partition).isEqualTo("from-config-file");
	}

	@Test
	public void notAllRequiredTeamscaleOptionsSet() {
		assertThatCode(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-project=proj")
		).doesNotThrowAnyException();
		assertThatCode(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p")
		).doesNotThrowAnyException();
		assertThatCode(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token")
		).doesNotThrowAnyException();

		assertThatThrownBy(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-project=proj")
		).hasMessageContaining("You configured a 'teamscale-project' but no 'teamscale-partition' to upload to.");

		assertThatThrownBy(
				() -> parseAndThrow("teamscale-server-url=teamscale.com")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parseAndThrow("teamscale-server-url=teamscale.com,teamscale-user=user")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parseAndThrow("teamscale-server-url=teamscale.com,teamscale-access-token=token")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parseAndThrow("teamscale-user=user,teamscale-access-token=token")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parseAndThrow("teamscale-revision=1234")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parseAndThrow("teamscale-commit=master:1234")
		).hasMessageContaining("not all required ones");
	}

	@Test
	public void sapNwdiRequiresAllTeamscaleOptionsExceptProject() {
		assertThatThrownBy(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
		).hasMessageContaining(
				"You provided an SAP NWDI applications config, but the 'teamscale-' upload options are incomplete");

		assertThatThrownBy(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-project=proj,sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
		).hasMessageContaining(
				"The project must be specified via sap-nwdi-applications");

		assertThatThrownBy(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-project=proj,sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
		).hasMessageContaining(
						"You provided an SAP NWDI applications config and a teamscale-project")
				.hasMessageNotContaining("the 'teamscale-' upload options are incomplete");
	}

	@Test
	public void revisionOrCommitRequireProject() {
		assertThatThrownBy(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-revision=12345")
		).hasMessageContaining("you did not provide the 'teamscale-project'");
		assertThatThrownBy(
				() -> parseAndThrow(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-commit=master:HEAD")
		).hasMessageContaining("you did not provide the 'teamscale-project'");
	}

	@Test
	public void environmentConfigIdDoesNotExist() {
		mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("invalid-config-id does not exist"));
		assertThatThrownBy(
				() -> new AgentOptionsParser(new CommandLineLogger(), "invalid-config-id", null,
						teamscaleCredentials).parse(
						"")
		).isInstanceOf(AgentOptionParseException.class).hasMessageContaining("invalid-config-id does not exist");
	}

	@Test
	public void notGivingAnyOptionsShouldBeOK() throws Exception {
		parseAndThrow("");
		parseAndThrow(null);
	}

	@Test
	public void mustPreserveDefaultExcludes() throws Exception {
		assertThat(parseAndThrow("").jacocoExcludes).isEqualTo(AgentOptions.DEFAULT_EXCLUDES);
		assertThat(parseAndThrow("excludes=**foo**").jacocoExcludes)
				.isEqualTo("**foo**:" + AgentOptions.DEFAULT_EXCLUDES);
	}

	@Test
	public void teamscalePropertiesCredentialsUsedAsDefaultButOverridable() throws Exception {
		assertThat(parseAndThrow(new AgentOptionsParser(new CommandLineLogger(), null, null, teamscaleCredentials), "teamscale-project=p,teamscale-partition=p").teamscaleServer.userName).isEqualTo(
				"user");
		assertThat(parseAndThrow(new AgentOptionsParser(new CommandLineLogger(), null, null, teamscaleCredentials),
				"teamscale-project=p,teamscale-partition=p,teamscale-user=user2").teamscaleServer.userName).isEqualTo(
				"user2");
	}

	private AgentOptions parseAndThrow(AgentOptionsParser parser, String options) throws Exception {
		AgentOptions result = parser.parse(options);
		parser.throwOnCollectedErrors();
		return result;
	}

	private AgentOptions parseAndThrow(String options) throws Exception {
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), null, null, null);
		return parseAndThrow(parser, options);
	}

	/**
	 * Delete created coverage folders
	 */
	@AfterAll
	public static void teardown() throws IOException {
		TestUtils.cleanAgentCoverageDirectory();
	}
}
