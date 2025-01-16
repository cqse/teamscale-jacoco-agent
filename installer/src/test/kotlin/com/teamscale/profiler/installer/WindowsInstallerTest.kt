package com.teamscale.profiler.installer

import com.teamscale.profiler.installer.Installer.UninstallerErrorReporter
import com.teamscale.profiler.installer.utils.MockRegistry
import com.teamscale.profiler.installer.utils.MockTeamscale
import com.teamscale.profiler.installer.utils.UninstallErrorReporterAssert
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
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@EnabledOnOs(OS.WINDOWS)
internal class WindowsInstallerTest {
	private lateinit var sourceDirectory: Path
	private lateinit var targetDirectory: Path

	private lateinit var installedAgentLibrary: Path

	private lateinit var registry: MockRegistry

	@BeforeEach
	@Throws(IOException::class)
	fun setUpSourceDirectory() {
		sourceDirectory = Files.createTempDirectory("InstallerTest-source")
		targetDirectory = Files.createTempDirectory("InstallerTest-target").resolve("profiler")

		val fileToInstall = sourceDirectory.resolve("install-me.txt")
		Files.writeString(fileToInstall, FILE_TO_INSTALL_CONTENT, StandardOpenOption.CREATE)

		val nestedFileToInstall = sourceDirectory.resolve("lib/teamscale-jacoco-agent.jar")
		Files.createDirectories(nestedFileToInstall.parent)
		Files.writeString(
			nestedFileToInstall, NESTED_FILE_CONTENT,
			StandardOpenOption.CREATE
		)

		installedAgentLibrary = targetDirectory.resolve("lib/teamscale-jacoco-agent.jar")

		registry = MockRegistry()
	}

	@Test
	@Throws(FatalInstallerError::class)
	fun successfulInstallation() {
		install()

		Assertions.assertThat(registry.getVariable("_JAVA_OPTIONS")).isEqualTo("-javaagent:$installedAgentLibrary")
		Assertions.assertThat(registry.getVariable("JAVA_TOOL_OPTIONS"))
			.isEqualTo("-javaagent:$installedAgentLibrary")
	}

	@Test
	@Throws(FatalInstallerError::class)
	fun successfulUninstallation() {
		install()
		val errorReporter = uninstall()
		UninstallErrorReporterAssert.assertThat(errorReporter).hadNoErrors()

		Assertions.assertThat(registry.getVariable("_JAVA_OPTIONS")).isNull()
		Assertions.assertThat(registry.getVariable("JAVA_TOOL_OPTIONS")).isNull()
	}

	@Throws(FatalInstallerError::class)
	private fun install() {
		Installer(sourceDirectory, targetDirectory, Paths.get("/etc"), false, registry).runInstall(
			TeamscaleCredentials(TEAMSCALE_URL.toHttpUrl(), "user", "accesskey")
		)
	}

	private fun uninstall(): UninstallerErrorReporter {
		return Installer(
			sourceDirectory, targetDirectory,
			Paths.get("/etc"), false, registry
		).runUninstall()
	}

	companion object {
		private const val TEAMSCALE_PORT = 8059
		private const val FILE_TO_INSTALL_CONTENT = "install-me"
		private const val NESTED_FILE_CONTENT = "nested-file"
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
