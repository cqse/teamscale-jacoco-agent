package com.teamscale.profiler.installer;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallerTest {

	private static final int TEAMSCALE_PORT = 8054;
	public static final String FILE_TO_INSTALL_CONTENT = "install-me";
	public static final String NESTED_FILE_CONTENT = "nested-file";

	private Path sourceDirectory;
	private Path targetDirectory;

	private Path fileToInstall;
	private Path nestedFileToInstall;

	private Path installedFile;
	private Path installedNestedFile;
	private Path installedTeamscaleProperties;
	private Path installedCoverageDirectory;
	private Path installedLogsDirectory;

	private MockTeamscale teamscale;

	@BeforeEach
	void setUpSourceDirectory() throws IOException {
		sourceDirectory = Files.createTempDirectory("InstallerTest-source");
		targetDirectory = Files.createTempDirectory("InstallerTest-target").resolve("profiler");

		fileToInstall = sourceDirectory.resolve("install-me.txt");
		Files.write(fileToInstall, FILE_TO_INSTALL_CONTENT.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

		nestedFileToInstall = sourceDirectory.resolve("subfolder/nested-file.txt");
		Files.createDirectories(nestedFileToInstall.getParent());
		Files.write(nestedFileToInstall, NESTED_FILE_CONTENT.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE);

		installedFile = targetDirectory.resolve(sourceDirectory.relativize(fileToInstall));
		installedNestedFile = targetDirectory.resolve(sourceDirectory.relativize(nestedFileToInstall));
		installedTeamscaleProperties = targetDirectory.resolve("teamscale.properties");
		installedCoverageDirectory = targetDirectory.resolve("coverage");
		installedLogsDirectory = targetDirectory.resolve("logs");
	}

	@BeforeEach
	void startFakeTeamscale() {
		teamscale = new MockTeamscale(TEAMSCALE_PORT);
	}

	@AfterEach
	void stopFakeTeamscale() {
		teamscale.shutdown();
	}

	@Test
	void successfulInstallation() throws FatalInstallerError, IOException {
		String url = "http://localhost:" + TEAMSCALE_PORT + "/";
		install(url);

		assertThat(installedFile).exists().content().isEqualTo(FILE_TO_INSTALL_CONTENT);
		assertThat(installedNestedFile).exists().content().isEqualTo(NESTED_FILE_CONTENT);

		assertThat(installedTeamscaleProperties).exists();
		Properties properties = FileSystemUtils.readProperties(installedTeamscaleProperties.toFile());
		assertThat(properties.keySet()).containsExactlyInAnyOrder("url", "username", "accesskey");
		assertThat(properties.getProperty("url")).isEqualTo(url);
		assertThat(properties.getProperty("username")).isEqualTo("user");
		assertThat(properties.getProperty("accesskey")).isEqualTo("accesskey");

		assertThat(installedCoverageDirectory).exists().isReadable().isWritable();
		assertThat(installedLogsDirectory).exists().isReadable().isWritable();
	}

	@Test
	void nonexistantTeamscaleUrl() {
		assertThatThrownBy(() -> install("http://does-not-exist:8080")).hasMessageContaining("could not be resolved");

		assertThat(targetDirectory).doesNotExist();
	}

	private void install(String teamscaleUrl) throws FatalInstallerError {
		new Installer(sourceDirectory, targetDirectory).run(new String[]{teamscaleUrl, "user", "accesskey"});
	}

}
