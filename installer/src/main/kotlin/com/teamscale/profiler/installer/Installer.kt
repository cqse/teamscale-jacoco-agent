package com.teamscale.profiler.installer

import com.teamscale.profiler.installer.steps.*
import com.teamscale.profiler.installer.steps.IStep.IUninstallErrorReporter
import com.teamscale.profiler.installer.utils.TeamscaleUtils.checkTeamscaleConnection
import com.teamscale.profiler.installer.windows.IRegistry
import com.teamscale.profiler.installer.windows.WindowsRegistry
import org.apache.commons.lang3.SystemUtils
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/** Installs the agent system-globally.
 * @param sourceDirectory  directory that contains the profiler binaries and support files to install.
 * @param installDirectory directory to which to install the profiler.
 * @param etcDirectory     on Linux: the /etc directory
 * @param registry         the Windows registry (not used on Linux) */
class Installer(
	sourceDirectory: Path,
	installDirectory: Path,
	etcDirectory: Path,
	reloadSystemdDaemon: Boolean,
	registry: IRegistry
) {
	/**
	 * List of steps to run during installation and uninstallation. During uninstallation, steps are run in the reverse
	 * order of this list. After each step, the system must be in a safe state, meaning the user's applications must
	 * still run. This is especially important during uninstallation.
	 */
	private val steps: List<IStep>

	/**
	 * Making the following directories configurable allows testing the installer without admin
	 * permissions.
	 */
	init {
		val environmentVariables = installDirectory.environmentVariables()
		steps = listOf(
			InstallAgentFilesStep(sourceDirectory, installDirectory),
			InstallWindowsSystemEnvironmentStep(environmentVariables, registry),
			InstallEtcEnvironmentStep(etcDirectory, environmentVariables),
			InstallSystemdStep(etcDirectory, environmentVariables, reloadSystemdDaemon)
		)
	}

	/**
	 * Installs the profiler.
	 *
	 * @throws FatalInstallerError if a step of the installation process fails.
	 */
	@Throws(FatalInstallerError::class)
	fun runInstall(credentials: TeamscaleCredentials) {
		credentials.checkTeamscaleConnection()
		steps.filter { it.shouldRun() }.forEach { step ->
			step.install(credentials)
		}
	}

	/**
	 * Uninstalls the profiler. All errors that happened during the uninstallation are reported via the returned
	 * [UninstallerErrorReporter].
	 */
	fun runUninstall(): UninstallerErrorReporter {
		val errorReporter = UninstallerErrorReporter()
		steps.asReversed()
			.filter { it.shouldRun() }
			.forEach { step ->
				step.uninstall(errorReporter)
				if (errorReporter.errorsReported) return errorReporter
			}
		return errorReporter
	}

	/**
	 * Reports errors during installation to stderr.
	 */
	class UninstallerErrorReporter : IUninstallErrorReporter {
		/** Indicates whether errors have been reported during the process. */
		var errorsReported: Boolean = false
		/** A flag indicating whether a permission error occurred during the uninstall process. */
		var hadPermissionError: Boolean = false

		/** Whether at least one error was reported.  */
		fun wereErrorsReported() = errorsReported

		override fun report(e: FatalInstallerError) {
			errorsReported = true
			if (e is PermissionError) {
				hadPermissionError = true
			}
			e.printToStderr()
		}
	}

	/**
	 * Returns the environment variables to set system-wide to register the agent. We currently set two options:
	 *
	 *  * JAVA_TOOL_OPTIONS is recognized by all JVMs but may be overridden by application start scripts
	 *  * _JAVA_OPTIONS is not officially documented but currently well-supported and unlikely to be used
	 * by application start scripts
	 */
	private fun Path.environmentVariables(): JvmEnvironmentMap {
		val javaAgentArgument = "-javaagent:" + getAgentJarPath(this)
		return JvmEnvironmentMap(
			"JAVA_TOOL_OPTIONS", javaAgentArgument,
			"_JAVA_OPTIONS", javaAgentArgument
		)
	}

	private fun getAgentJarPath(installDirectory: Path) =
		installDirectory.resolve("lib/teamscale-jacoco-agent.jar")

	companion object {
		private val DEFAULT_INSTALL_DIRECTORY = windowsOrLinux(
			{ Paths.get(System.getenv("ProgramFiles")).resolve("teamscale-profiler/java") },
			{ Paths.get("/opt/teamscale-profiler/java") }
		)

		private val DEFAULT_ETC_DIRECTORY = Paths.get("/etc")

		private val RERUN_ADVICE = windowsOrLinux(
			{ "Try running this installer as Administrator." },
			{ "Try running this installer as root, e.g. with sudo." }
		)

		private val RESTART_ADVICE = windowsOrLinux(
			{ "Please restart Windows to apply all changes." },
			{ "In an interactive session, you have to log out and log back in for the changes to take effect." }
		)

		private fun <T> windowsOrLinux(windowsSupplier: () -> T, linuxSupplier: () -> T) =
			if (SystemUtils.IS_OS_WINDOWS) {
				windowsSupplier()
			} else {
				linuxSupplier()
			}

		@get:Throws(FatalInstallerError::class)
		private val defaultSourceDirectory: Path
			/** Returns the directory that contains the agent to install or null if it can't be resolved.  */
			get() {
				// since we package with jlink, java.home is guaranteed to point to SOURCEDIR/installer/installer-PLATFORM
				val jlinkJvmPath = Paths.get(System.getProperty("java.home"))
				if (!Files.exists(jlinkJvmPath)) {
					throw FatalInstallerError(
						"""
						The JLink JVM path $jlinkJvmPath does not exist. It looks like you moved the installation files after extracting the zip.
						Please start over by extracting the profiler files from the zip file you downloaded. Do not make any changes to the extracted files and directories or installation will fail.
						""".trimIndent()
					)
				}

				val sourceDirectory = jlinkJvmPath.parent.parent
				if (!Files.exists(sourceDirectory)) {
					throw FatalInstallerError(
						"""
						The source directory $sourceDirectory does not exist. It looks like you moved the installation files after extracting the zip.
						Please start over by extracting the profiler files from the zip file you downloaded. Do not make any changes to the extracted files and directories or installation will fail.
						""".trimIndent()
					)
				}

				return sourceDirectory
			}

		/**
		 * Installs the profiler with the given Teamscale credentials.
		 *
		 * @return the exit code for the CLI.
		 */
		fun install(credentials: TeamscaleCredentials): Int {
			try {
				defaultInstaller.runInstall(credentials)
				println(
					"""
					Installation successful. Profiler installed to $DEFAULT_INSTALL_DIRECTORY
	
					To activate the profiler for an application, set the environment variable:
					TEAMSCALE_JAVA_PROFILER_CONFIG_ID
					Its value must be a valid profiler configuration ID defined in the Teamscale instance.
					Then, restart your application (for web applications: restart the app server).
					
					$RESTART_ADVICE
					""".trimIndent()
				)
				return CommandLine.ExitCode.OK
			} catch (e: PermissionError) {
				e.printToStderr()

				System.err.println(
					"""
					
					
					Installation failed because the installer had insufficient permissions to make the necessary changes on your system.
					See above for error messages.
					
					$RERUN_ADVICE
					""".trimIndent()
				)
				return RootCommand.EXIT_CODE_PERMISSION_ERROR
			} catch (e: FatalInstallerError) {
				e.printToStderr()
				System.err.println("\n\nInstallation failed. See above for error messages.")
				return RootCommand.EXIT_CODE_OTHER_ERROR
			}
		}

		@get:Throws(FatalInstallerError::class)
		private val defaultInstaller: Installer
			get() = Installer(
				defaultSourceDirectory,
				DEFAULT_INSTALL_DIRECTORY,
				DEFAULT_ETC_DIRECTORY,
				true,
				WindowsRegistry
			)

		/**
		 * Uninstalls the profiler.
		 *
		 * @return the exit code for the CLI.
		 */
		@Throws(FatalInstallerError::class)
		fun uninstall(): Int {
			val errorReporter = defaultInstaller.runUninstall()
			if (errorReporter.errorsReported) {
				var message = "Uninstallation failed. See above for error messages."
				if (errorReporter.hadPermissionError) {
					message += """
						
						$RERUN_ADVICE
						""".trimIndent()
				}
				System.err.println(
					"""
						
						
						$message
						""".trimIndent()
				)
				return RootCommand.EXIT_CODE_OTHER_ERROR
			}
			println(
				"""
				Profiler successfully uninstalled. Please restart your computer.
				You need to restart all previously profiled applications to stop profiling them.
				""".trimIndent()
			)
			return CommandLine.ExitCode.OK
		}
	}
}
