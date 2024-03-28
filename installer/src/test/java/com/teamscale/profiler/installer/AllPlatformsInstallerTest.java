package com.teamscale.profiler.installer;

import com.teamscale.profiler.installer.utils.MockRegistry;
import com.teamscale.profiler.installer.utils.MockTeamscale;
import com.teamscale.profiler.installer.utils.TestUtils;
import com.teamscale.test.commons.SystemTestUtils;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import static com.teamscale.profiler.installer.utils.UninstallErrorReporterAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllPlatformsInstallerTest {

	private static final String FILE_TO_INSTALL_CONTENT = "install-me";
	private static final String NESTED_FILE_CONTENT = "nested-file";
	private static final String TEAMSCALE_URL = "http://localhost:" + SystemTestUtils.TEAMSCALE_PORT + "/";


	private Path sourceDirectory;
	private Path targetDirectory;
	private Path etcDirectory;

	private Path installedFile;
	private Path installedNestedFile;
	private Path installedTeamscaleProperties;

	private static MockTeamscale mockTeamscale;

	@BeforeEach
	void setUpSourceDirectory() throws IOException {
		sourceDirectory = Files.createTempDirectory("InstallerTest-source");
		targetDirectory = Files.createTempDirectory("InstallerTest-target").resolve("profiler");
		etcDirectory = Files.createTempDirectory("InstallerTest-etc");

		Path fileToInstall = sourceDirectory.resolve("install-me.txt");
		Files.writeString(fileToInstall, FILE_TO_INSTALL_CONTENT, StandardOpenOption.CREATE);

		Path nestedFileToInstall = sourceDirectory.resolve("lib/teamscale-jacoco-agent.jar");
		Files.createDirectories(nestedFileToInstall.getParent());
		Files.writeString(nestedFileToInstall, NESTED_FILE_CONTENT,
				StandardOpenOption.CREATE);

		installedFile = targetDirectory.resolve(sourceDirectory.relativize(fileToInstall));
		installedNestedFile = targetDirectory.resolve(sourceDirectory.relativize(nestedFileToInstall));
		installedTeamscaleProperties = targetDirectory.resolve("teamscale.properties");
	}

	@BeforeAll
	static void startFakeTeamscale() {
		mockTeamscale = new MockTeamscale(SystemTestUtils.TEAMSCALE_PORT);
	}

	@AfterAll
	static void stopFakeTeamscale() {
		mockTeamscale.shutdown();
	}

	@Test
	void successfulInstallation() throws FatalInstallerError, IOException {
		install();

		assertThat(installedFile).exists().content().isEqualTo(FILE_TO_INSTALL_CONTENT);
		assertThat(installedNestedFile).exists().content().isEqualTo(NESTED_FILE_CONTENT);
		assertThat(installedTeamscaleProperties).exists();

		Properties properties = new Properties();
		properties.load(Files.newInputStream(installedTeamscaleProperties));
		assertThat(properties.keySet()).containsExactlyInAnyOrder("url", "username", "accesskey");
		assertThat(properties.getProperty("url")).isEqualTo(TEAMSCALE_URL);
		assertThat(properties.getProperty("username")).isEqualTo("user");
		assertThat(properties.getProperty("accesskey")).isEqualTo("accesskey");
	}

	@Test
	void distributionChangedByUser() throws IOException {
		Files.delete(sourceDirectory.resolve("lib/teamscale-jacoco-agent.jar"));
		assertThatThrownBy(this::install)
				.hasMessageContaining("It looks like you moved the installer");
	}

	@Test
	void successfulUninstallation() throws FatalInstallerError {
		install();
		Installer.UninstallerErrorReporter errorReporter = uninstall();
		assertThat(errorReporter).hadNoErrors();
	}

	@Test
	void nonexistantTeamscaleUrl() {
		assertThatThrownBy(() -> install("http://does-not-exist:8080"))
				.hasMessageContaining("could not be resolved");
		assertThat(targetDirectory).doesNotExist();
	}

	@Test
	void connectionRefused() {
		assertThatThrownBy(() -> install("http://localhost:" + (SystemTestUtils.TEAMSCALE_PORT + 1)))
				.hasMessageContaining("refused a connection");
		assertThat(targetDirectory).doesNotExist();
	}

	@Test
	void httpsInsteadOfHttp() {
		assertThatThrownBy(() -> install("https://localhost:" + (int) SystemTestUtils.TEAMSCALE_PORT))
				.hasMessageContaining("configured for HTTPS, not HTTP");
		assertThat(targetDirectory).doesNotExist();
	}

	@Test
	void profilerAlreadyInstalled() throws IOException {
		Files.createDirectories(targetDirectory);
		assertThatThrownBy(this::install).hasMessageContaining("Path already exists");
	}

	@Test
	void installDirectoryNotWritable() throws Exception {
		TestUtils.makePathReadOnly(targetDirectory.getParent());
		assertThatThrownBy(() -> install(TEAMSCALE_URL)).hasMessageContaining("Cannot create directory");
	}

	private void install() throws FatalInstallerError {
		install(TEAMSCALE_URL);
	}

	private void install(String teamscaleUrl) throws FatalInstallerError {
		new Installer(sourceDirectory, targetDirectory, etcDirectory, false, new MockRegistry()).runInstall(
				new TeamscaleCredentials(HttpUrl.get(teamscaleUrl), "user", "accesskey"));
	}

	private Installer.UninstallerErrorReporter uninstall() {
		return new Installer(sourceDirectory, targetDirectory, etcDirectory, false, new MockRegistry()).runUninstall();
	}

}
