package com.teamscale.jacoco.agent.options;

import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryConfig;
import com.teamscale.jacoco.agent.util.TestUtils;
import com.teamscale.report.util.CommandLineLogger;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests parsing of the agent's command line options. */
public class AgentOptionsParserTest {

	private final AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), null, null);

	@Test
	public void testUploadMethodRecognition() throws Exception {
		assertThat(parser.parse(null).determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.LOCAL_DISK);
		assertThat(parser.parse("upload-url=teamscale.url.com:443").determineUploadMethod()).isEqualTo(
				AgentOptions.EUploadMethod.HTTP);
		assertThat(parser.parse("azure-url=azure.com,azure-key=key").determineUploadMethod()).isEqualTo(
				AgentOptions.EUploadMethod.AZURE_FILE_STORAGE);
		assertThat(parser.parse(
				String.format("%s=%s,%s=%s,%s=%s", ArtifactoryConfig.ARTIFACTORY_URL_OPTION, "http://some_url",
						ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION, "apikey",
						ArtifactoryConfig.ARTIFACTORY_PARTITION, "partition")
		).determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.ARTIFACTORY);

		String basicTeamscaleOptions = "teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p";
		assertThat(parser.parse(basicTeamscaleOptions)
				.determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.TEAMSCALE_MULTI_PROJECT);
		assertThat(parser.parse(basicTeamscaleOptions + ",teamscale-project=proj")
				.determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.TEAMSCALE_SINGLE_PROJECT);
		assertThat(parser.parse(
						basicTeamscaleOptions + ",sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
				.determineUploadMethod())
				.isEqualTo(AgentOptions.EUploadMethod.SAP_NWDI_TEAMSCALE);
	}

	@Test
	public void testUploadMethodRecognitionWithTeamscaleProperties() throws Exception {
		TeamscaleCredentials credentials = new TeamscaleCredentials(HttpUrl.get("http://localhost"), "user", "key");
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), null, credentials);

		assertThat(parser.parse(null).determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.LOCAL_DISK);
		assertThat(parser.parse("upload-url=teamscale.url.com:443").determineUploadMethod()).isEqualTo(
				AgentOptions.EUploadMethod.HTTP);
		assertThat(parser.parse("azure-url=azure.com,azure-key=key").determineUploadMethod()).isEqualTo(
				AgentOptions.EUploadMethod.AZURE_FILE_STORAGE);
		assertThat(parser.parse(
				String.format("%s=%s,%s=%s,%s=%s", ArtifactoryConfig.ARTIFACTORY_URL_OPTION, "http://some_url",
						ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION, "apikey",
						ArtifactoryConfig.ARTIFACTORY_PARTITION, "partition")
		).determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.ARTIFACTORY);

		String basicTeamscaleOptions = "teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p";
		assertThat(parser.parse(basicTeamscaleOptions)
				.determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.TEAMSCALE_MULTI_PROJECT);
		assertThat(parser.parse(basicTeamscaleOptions + ",teamscale-project=proj")
				.determineUploadMethod()).isEqualTo(AgentOptions.EUploadMethod.TEAMSCALE_SINGLE_PROJECT);
		assertThat(parser.parse(
						basicTeamscaleOptions + ",sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
				.determineUploadMethod())
				.isEqualTo(AgentOptions.EUploadMethod.SAP_NWDI_TEAMSCALE);
	}

	@Test
	public void environmentConfigOverridesCommandLineOptions(
			@TempDir Path tempDir) throws IOException, AgentOptionParseException {
		Path config = tempDir.resolve("config.properties");
		Files.write(config, "debug=true".getBytes(StandardCharsets.UTF_8));
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), config.toString(), null);
		AgentOptions options = parser.parse("debug=false");

		assertThat(options.debugLogging).isEqualTo(true);
	}

	@Test
	public void notAllRequiredTeamscaleOptionsSet() {
		assertThatCode(
				() -> parser.parse(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-project=proj")
		).doesNotThrowAnyException();
		assertThatCode(
				() -> parser.parse(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p")
		).doesNotThrowAnyException();
		assertThatCode(
				() -> parser.parse(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token")
		).doesNotThrowAnyException();

		assertThatThrownBy(
				() -> parser.parse(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-project=proj")
		).hasMessageContaining("You configured a teamscale-project but no teamscale-partition to upload to.");

		assertThatThrownBy(
				() -> parser.parse("teamscale-server-url=teamscale.com")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parser.parse("teamscale-server-url=teamscale.com,teamscale-user=user")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parser.parse("teamscale-server-url=teamscale.com,teamscale-access-token=token")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parser.parse("teamscale-user=user,teamscale-access-token=token")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parser.parse("teamscale-revision=1234")
		).hasMessageContaining("not all required ones");
		assertThatThrownBy(
				() -> parser.parse("teamscale-commit=master:1234")
		).hasMessageContaining("not all required ones");
	}

	@Test
	public void sapNwdiRequiresAllTeamscaleOptionsExceptProject() {
		assertThatThrownBy(
				() -> parser.parse(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
		).hasMessageContaining(
				"You provided an SAP NWDI applications config, but the 'teamscale-' upload options are incomplete");
		assertThatThrownBy(
				() -> parser.parse(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-project=proj,sap-nwdi-applications=com.package.MyClass:projectId;com.company.Main:project")
		).hasMessageContaining(
				"The project must be specified via sap-nwdi-applications");
	}

	@Test
	public void revisionOrCommitRequireProject() {
		assertThatThrownBy(
				() -> parser.parse(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-revision=12345")
		).hasMessageContaining("you did not provide the 'teamscale-project'");
		assertThatThrownBy(
				() -> parser.parse(
						"teamscale-server-url=teamscale.com,teamscale-user=user,teamscale-access-token=token,teamscale-partition=p,teamscale-commit=master:HEAD")
		).hasMessageContaining("you did not provide the 'teamscale-project'");
	}

	@Test
	public void environmentConfigPathDoesNotExist() {
		assertThatThrownBy(
				() -> new AgentOptionsParser(new CommandLineLogger(), "/this/file/doesnt/exist", null).parse("")
		).hasMessageContaining("not found");
	}

	@Test
	public void notGivingAnyOptionsShouldBeOK() throws Exception {
		parser.parse("");
		parser.parse(null);
	}

	@Test
	public void mustPreserveDefaultExcludes() throws Exception {
		assertThat(parser.parse("").jacocoExcludes).isEqualTo(AgentOptions.DEFAULT_EXCLUDES);
		assertThat(parser.parse("excludes=**foo**").jacocoExcludes)
				.isEqualTo("**foo**:" + AgentOptions.DEFAULT_EXCLUDES);
	}

	@Test
	public void mustDoHttpsRewriteForNoSchemePort443() throws Exception {
		assertThat(parser.parse(
				"upload-url=teamscale.url.com:443").uploadUrl)
				.isEqualTo(HttpUrl.parse("https://teamscale.url.com:443/"));
	}

	@Test
	public void mustNotDoHttpsRewriteForSchemePort443() throws Exception {
		assertThat(parser.parse(
				"upload-url=http://teamscale.url.com:443").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:443/"));
	}

	@Test
	public void defaultHttpRewriteForNoScheme() throws Exception {
		assertThat(parser.parse(
				"upload-url=teamscale.url.com:8080").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:8080/"));
		assertThat(parser.parse(
				"upload-url=teamscale.url.com:80").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:80/"));
		assertThat(parser.parse(
				"upload-url=teamscale.url.com:444").uploadUrl)
				.isEqualTo(HttpUrl.parse("http://teamscale.url.com:444/"));
	}

	@Test
	public void teamscalePropertiesCredentialsUsedAsDefaultButOverridable() throws Exception {
		TeamscaleCredentials credentials = new TeamscaleCredentials(HttpUrl.get("http://localhost"), "user", "key");
		AgentOptionsParser parser = new AgentOptionsParser(new CommandLineLogger(), null, credentials);

		assertThat(parser.parse("teamscale-project=p,teamscale-partition=p").teamscaleServer.userName).isEqualTo(
				"user");
		assertThat(parser.parse(
				"teamscale-project=p,teamscale-partition=p,teamscale-user=user2").teamscaleServer.userName).isEqualTo(
				"user2");
	}

	/**
	 * Delete created coverage folders
	 */
	@AfterAll
	public static void teardown() throws IOException {
		TestUtils.cleanAgentCoverageDirectory();
	}
}
