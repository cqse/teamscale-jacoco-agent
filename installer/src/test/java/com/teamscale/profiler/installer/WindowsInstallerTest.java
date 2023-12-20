package com.teamscale.profiler.installer;

import com.teamscale.profiler.installer.utils.MockRegistry;
import com.teamscale.profiler.installer.utils.MockTeamscale;
import com.teamscale.profiler.installer.utils.UninstallErrorReporterAssert;
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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledOnOs(OS.WINDOWS)
class WindowsInstallerTest {

	private static final int TEAMSCALE_PORT = 8059;
	private static final String FILE_TO_INSTALL_CONTENT = "install-me";
	private static final String NESTED_FILE_CONTENT = "nested-file";
	private static final String TEAMSCALE_URL = "http://localhost:" + TEAMSCALE_PORT + "/";


	private Path sourceDirectory;
	private Path targetDirectory;

	private Path installedAgentLibrary;

	private static MockTeamscale mockTeamscale;

	private MockRegistry registry;

	@BeforeEach
	void setUpSourceDirectory() throws IOException {
		sourceDirectory = Files.createTempDirectory("InstallerTest-source");
		targetDirectory = Files.createTempDirectory("InstallerTest-target").resolve("profiler");

		Path fileToInstall = sourceDirectory.resolve("install-me.txt");
		Files.write(fileToInstall, FILE_TO_INSTALL_CONTENT.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

		Path nestedFileToInstall = sourceDirectory.resolve("lib/teamscale-jacoco-agent.jar");
		Files.createDirectories(nestedFileToInstall.getParent());
		Files.write(nestedFileToInstall, NESTED_FILE_CONTENT.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE);

		installedAgentLibrary = targetDirectory.resolve("lib/teamscale-jacoco-agent.jar");

		registry = new MockRegistry();
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
	void successfulInstallation() throws FatalInstallerError {
		install();

		assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:" + installedAgentLibrary);
		assertThat(registry.getVariable("JAVA_TOOL_OPTIONS")).isEqualTo("-javaagent:" + installedAgentLibrary);
	}

	@Test
	void successfulUninstallation() throws FatalInstallerError {
		install();
		Installer.UninstallerErrorReporter errorReporter = uninstall();
		UninstallErrorReporterAssert.assertThat(errorReporter).hadNoErrors();

		assertThat(registry.getVariable("_JAVA_OPTIONS")).isNull();
		assertThat(registry.getVariable("JAVA_TOOL_OPTIONS")).isNull();
	}

	@Test
	void uninstallDeletingAgentDirectoryFails() throws FatalInstallerError {
		install();
		assertThat(targetDirectory.toFile().setWritable(false, false)).isTrue();

		Installer.UninstallerErrorReporter errorReporter = uninstall();
		UninstallErrorReporterAssert.assertThat(errorReporter).hadErrors();

		assertThat(registry.getVariable("_JAVA_OPTIONS")).isNull();
		assertThat(registry.getVariable("JAVA_TOOL_OPTIONS")).isNull();
	}

	private void install() throws FatalInstallerError {
		new Installer(sourceDirectory, targetDirectory, Paths.get("/etc"), false, registry).runInstall(
				new TeamscaleCredentials(HttpUrl.get(WindowsInstallerTest.TEAMSCALE_URL), "user", "accesskey"));
	}

	private Installer.UninstallerErrorReporter uninstall() {
		return new Installer(sourceDirectory, targetDirectory,
				Paths.get("/etc"), false, registry).runUninstall();
	}

}
