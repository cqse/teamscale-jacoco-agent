package com.teamscale.profiler.installer;

import com.teamscale.profiler.installer.utils.MockTeamscale;
import com.teamscale.profiler.installer.windows.WindowsRegistry;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static com.teamscale.profiler.installer.utils.UninstallErrorReporterAssert.assertThat;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledOnOs(OS.LINUX)
class LinuxInstallerTest {

	private static final int TEAMSCALE_PORT = 8059;
	private static final String FILE_TO_INSTALL_CONTENT = "install-me";
	private static final String NESTED_FILE_CONTENT = "nested-file";
	private static final String ENVIRONMENT_CONTENT = "#this is /etc/environment\nPATH=/usr/bin";
	private static final String TEAMSCALE_URL = "http://localhost:" + TEAMSCALE_PORT + "/";


	private Path sourceDirectory;
	private Path targetDirectory;
	private Path etcDirectory;

	private Path installedFile;
	private Path installedTeamscaleProperties;
	private Path installedAgentLibrary;

	private Path environmentFile;
	private Path systemdDirectory;
	private Path systemdConfig;

	private static MockTeamscale mockTeamscale;

	@BeforeEach
	void setUpSourceDirectory() throws IOException {
		sourceDirectory = Files.createTempDirectory("InstallerTest-source");
		targetDirectory = Files.createTempDirectory("InstallerTest-target").resolve("profiler");
		etcDirectory = Files.createTempDirectory("InstallerTest-etc");

		environmentFile = etcDirectory.resolve("environment");
		Files.write(environmentFile, ENVIRONMENT_CONTENT.getBytes(StandardCharsets.UTF_8));

		systemdDirectory = etcDirectory.resolve("systemd");
		Files.createDirectory(systemdDirectory);
		systemdConfig = systemdDirectory.resolve("system.conf.d/teamscale-java-profiler.conf");

		Path fileToInstall = sourceDirectory.resolve("install-me.txt");
		Files.write(fileToInstall, FILE_TO_INSTALL_CONTENT.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

		Path nestedFileToInstall = sourceDirectory.resolve("lib/teamscale-jacoco-agent.jar");
		Files.createDirectories(nestedFileToInstall.getParent());
		Files.write(nestedFileToInstall, NESTED_FILE_CONTENT.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE);

		installedFile = targetDirectory.resolve(sourceDirectory.relativize(fileToInstall));
		installedTeamscaleProperties = targetDirectory.resolve("teamscale.properties");
		installedAgentLibrary = targetDirectory.resolve("lib/teamscale-jacoco-agent.jar");
	}

	@BeforeAll
	static void startFakeTeamscale() {
		mockTeamscale = new MockTeamscale(TEAMSCALE_PORT);
	}

	@AfterAll
	static void stopFakeTeamscale() {
		mockTeamscale.shutdown();
	}

	@Test
	void successfulInstallation() throws FatalInstallerError, IOException {
		install();

		assertThat(Files.getPosixFilePermissions(installedTeamscaleProperties)).contains(OTHERS_READ);
		assertThat(Files.getPosixFilePermissions(installedFile)).contains(OTHERS_READ);

		assertThat(environmentFile).content().isEqualTo(ENVIRONMENT_CONTENT
				+ "\nJAVA_TOOL_OPTIONS=-javaagent:" + installedAgentLibrary
				+ "\n_JAVA_OPTIONS=-javaagent:" + installedAgentLibrary + "\n");

		assertThat(systemdConfig).content().isEqualTo("[Manager]"
				+ "\nDefaultEnvironment=JAVA_TOOL_OPTIONS=-javaagent:" + installedAgentLibrary
				+ " _JAVA_OPTIONS=-javaagent:" + installedAgentLibrary + "\n");
	}

	@Test
	void successfulUninstallation() throws FatalInstallerError {
		install();
		Installer.UninstallerErrorReporter errorReporter = uninstall();
		assertThat(errorReporter).hadNoErrors();

		assertThat(environmentFile).exists().content().isEqualTo(ENVIRONMENT_CONTENT);
		assertThat(systemdConfig).doesNotExist();
	}

	@Test
	void uninstallSuccessfullyEvenIfSystemDConfigWasManuallyRemoved() throws FatalInstallerError, IOException {
		install();
		Files.delete(systemdConfig);
		Installer.UninstallerErrorReporter errorReporter = uninstall();
		assertThat(errorReporter).hadNoErrors();
	}

	@Test
	void uninstallSuccessfullyEvenIfEnvironmentFileDoesntExist() throws FatalInstallerError, IOException {
		install();
		Files.delete(environmentFile);
		Installer.UninstallerErrorReporter errorReporter = uninstall();
		assertThat(errorReporter).hadNoErrors();
	}

	@Test
	void uninstallDeletingAgentDirectoryFails() throws FatalInstallerError {
		install();
		assertThat(targetDirectory.toFile().setWritable(false, false)).isTrue();

		Installer.UninstallerErrorReporter errorReporter = uninstall();
		assertThat(errorReporter).hadErrors();

		assertThat(environmentFile).exists().content().isEqualTo(ENVIRONMENT_CONTENT);
		assertThat(systemdConfig).doesNotExist();
	}

	@Test
	void uninstallChangingEtcEnvironmentFails() throws FatalInstallerError {
		install();
		assertThat(environmentFile.toFile().setWritable(false, false)).isTrue();

		Installer.UninstallerErrorReporter errorReporter = uninstall();
		assertThat(errorReporter).hadErrors();

		assertThat(environmentFile).exists().content().contains("_JAVA_OPTIONS");

		// ensure that the agent uninstall step did not run because the preceding environment step failed
		assertThat(targetDirectory).exists();
		assertThat(installedTeamscaleProperties).exists();
	}

	@Test
	void noEtcEnvironment() throws FatalInstallerError, IOException {
		Files.delete(environmentFile);
		install();

		assertThat(environmentFile).doesNotExist();
	}

	@Test
	void noSystemd() throws FatalInstallerError, IOException {
		Files.delete(systemdDirectory);
		install();

		assertThat(systemdConfig).doesNotExist();
	}

	private void install() throws FatalInstallerError {
		new Installer(sourceDirectory, targetDirectory, etcDirectory, false, WindowsRegistry.INSTANCE).runInstall(
				new TeamscaleCredentials(HttpUrl.get(LinuxInstallerTest.TEAMSCALE_URL), "user", "accesskey"));
	}

	private Installer.UninstallerErrorReporter uninstall() {
		return new Installer(sourceDirectory, targetDirectory,
				etcDirectory, false, WindowsRegistry.INSTANCE).runUninstall();
	}

}
