package com.teamscale.profiler.installer

import com.teamscale.profiler.installer.Installer.UninstallerErrorReporter
import com.teamscale.profiler.installer.utils.MockTeamscale
import com.teamscale.profiler.installer.utils.TestUtils
import com.teamscale.profiler.installer.utils.UninstallErrorReporterAssert
import com.teamscale.profiler.installer.windows.WindowsRegistry
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission

@EnabledOnOs(OS.LINUX)
internal class LinuxInstallerTest {
	private lateinit var sourceDirectory: Path
	private lateinit var targetDirectory: Path
	private lateinit var etcDirectory: Path

	private lateinit var installedFile: Path
	private lateinit var installedTeamscaleProperties: Path
	private lateinit var installedAgentLibrary: Path

	private lateinit var environmentFile: Path
	private lateinit var systemdDirectory: Path
	private lateinit var systemdConfig: Path

	@BeforeEach
	@Throws(IOException::class)
	fun setUpSourceDirectory() {
		sourceDirectory = Files.createTempDirectory("InstallerTest-source")
		targetDirectory = Files.createTempDirectory("InstallerTest-target").resolve("profiler")
		etcDirectory = Files.createTempDirectory("InstallerTest-etc")

		environmentFile = etcDirectory.resolve("environment")
		Files.writeString(environmentFile, ENVIRONMENT_CONTENT)

		systemdDirectory = etcDirectory.resolve("systemd")
		Files.createDirectory(systemdDirectory)
		systemdConfig = systemdDirectory.resolve("system.conf.d/teamscale-java-profiler.conf")

		val fileToInstall = sourceDirectory.resolve("install-me.txt")
		Files.writeString(fileToInstall, FILE_TO_INSTALL_CONTENT, StandardOpenOption.CREATE)

		val nestedFileToInstall = sourceDirectory.resolve("lib/teamscale-jacoco-agent.jar")
		Files.createDirectories(nestedFileToInstall.parent)
		Files.writeString(nestedFileToInstall, NESTED_FILE_CONTENT, StandardOpenOption.CREATE)

		installedFile = targetDirectory.resolve(sourceDirectory.relativize(fileToInstall))
		installedTeamscaleProperties = targetDirectory.resolve("teamscale.properties")
		installedAgentLibrary = targetDirectory.resolve("lib/teamscale-jacoco-agent.jar")
	}

	@Test
	@Throws(FatalInstallerError::class, IOException::class)
	fun successfulInstallation() {
		install()

		Assertions.assertThat(Files.getPosixFilePermissions(installedTeamscaleProperties))
			.contains(PosixFilePermission.OTHERS_READ)
		Assertions.assertThat(Files.getPosixFilePermissions(installedFile)).contains(PosixFilePermission.OTHERS_READ)

		Assertions.assertThat(environmentFile).content().isEqualToIgnoringWhitespace(
			"""
			$ENVIRONMENT_CONTENT
			JAVA_TOOL_OPTIONS=-javaagent:$installedAgentLibrary
			_JAVA_OPTIONS=-javaagent:$installedAgentLibrary
			"""
		)

		Assertions.assertThat(systemdConfig).content().isEqualToIgnoringWhitespace(
			"""
			[Manager]
			DefaultEnvironment=JAVA_TOOL_OPTIONS=-javaagent:$installedAgentLibrary
			_JAVA_OPTIONS=-javaagent:$installedAgentLibrary
			"""
		)
	}

	@Test
	@Throws(FatalInstallerError::class)
	fun successfulUninstallation() {
		install()
		val errorReporter = uninstall()
		UninstallErrorReporterAssert.assertThat(errorReporter).hadNoErrors()

		Assertions.assertThat(targetDirectory).doesNotExist()
		Assertions.assertThat(environmentFile).exists().content().isEqualTo(ENVIRONMENT_CONTENT)
		Assertions.assertThat(systemdConfig).doesNotExist()
	}

	@Test
	@Throws(FatalInstallerError::class, IOException::class)
	fun uninstallSuccessfullyEvenIfSystemDConfigWasManuallyRemoved() {
		install()
		Files.delete(systemdConfig)
		val errorReporter = uninstall()
		UninstallErrorReporterAssert.assertThat(errorReporter).hadNoErrors()
	}

	@Test
	@Throws(FatalInstallerError::class, IOException::class)
	fun uninstallSuccessfullyEvenIfEnvironmentFileDoesntExist() {
		install()
		Files.delete(environmentFile)
		val errorReporter = uninstall()
		UninstallErrorReporterAssert.assertThat(errorReporter).hadNoErrors()
	}

	@Test
	@Throws(Exception::class)
	fun uninstallDeletingAgentDirectoryFails() {
		install()
		TestUtils.makePathReadOnly(targetDirectory)
		TestUtils.makePathReadOnly(installedTeamscaleProperties)

		val errorReporter = uninstall()
		UninstallErrorReporterAssert.assertThat(errorReporter).hadErrors()

		Assertions.assertThat(targetDirectory).exists()
		Assertions.assertThat(installedTeamscaleProperties).exists()
		Assertions.assertThat(environmentFile).exists().content().isEqualTo(ENVIRONMENT_CONTENT)
		Assertions.assertThat(systemdConfig).doesNotExist()
	}

	@Test
	@Throws(Exception::class)
	fun uninstallChangingEtcEnvironmentFails() {
		install()
		TestUtils.makePathReadOnly(environmentFile)

		val errorReporter = uninstall()
		UninstallErrorReporterAssert.assertThat(errorReporter).hadErrors()

		Assertions.assertThat(environmentFile).exists().content().contains("_JAVA_OPTIONS")

		// ensure that the agent uninstall step did not run because the preceding environment step failed
		Assertions.assertThat(targetDirectory).exists()
		Assertions.assertThat(installedTeamscaleProperties).exists()
	}

	@Test
	@Throws(FatalInstallerError::class, IOException::class)
	fun noEtcEnvironment() {
		Files.delete(environmentFile)
		install()

		Assertions.assertThat(environmentFile).doesNotExist()
	}

	@Test
	@Throws(FatalInstallerError::class, IOException::class)
	fun noSystemd() {
		Files.delete(systemdDirectory)
		install()

		Assertions.assertThat(systemdConfig).doesNotExist()
	}

	@Throws(FatalInstallerError::class)
	private fun install() {
		Installer(sourceDirectory, targetDirectory, etcDirectory, false, WindowsRegistry.INSTANCE).runInstall(
			TeamscaleCredentials(TEAMSCALE_URL.toHttpUrl(), "user", "accesskey")
		)
	}

	private fun uninstall(): UninstallerErrorReporter {
		return Installer(
			sourceDirectory, targetDirectory,
			etcDirectory, false, WindowsRegistry.INSTANCE
		).runUninstall()
	}

	companion object {
		private const val TEAMSCALE_PORT = 8059
		private const val FILE_TO_INSTALL_CONTENT = "install-me"
		private const val NESTED_FILE_CONTENT = "nested-file"
		private const val ENVIRONMENT_CONTENT = "#this is /etc/environment\nPATH=/usr/bin"
		private const val TEAMSCALE_URL = "http://localhost:$TEAMSCALE_PORT/"


		private var mockTeamscale: MockTeamscale? = null

		@JvmStatic
		@BeforeAll
		fun startFakeTeamscale() {
			mockTeamscale = MockTeamscale(TEAMSCALE_PORT)
		}

		@JvmStatic
		@AfterAll
		fun stopFakeTeamscale() {
			mockTeamscale?.shutdown()
		}
	}
}
