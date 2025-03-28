package com.teamscale.profiler.installer

import com.teamscale.profiler.installer.utils.TeamscaleUtils
import org.apache.commons.lang3.SystemUtils
import picocli.CommandLine
import kotlin.system.exitProcess

/**
 * The main command for the CLI. Does nothing itself but hosts the subcommands.
 */
@CommandLine.Command(
	name = "installer",
	mixinStandardHelpOptions = true,
	usageHelpAutoWidth = true,
	scope = CommandLine.ScopeType.INHERIT,
	subcommands = [InstallCommand::class, UninstallCommand::class],
	versionProvider = VersionProvider::class,
	description = ["Installs or uninstalls the profiler system-wide. Must be run as root/Administrator."]
)
object RootCommand {
	/**
	 * Disables SSL certificate validation during the installation process to allow the use of
	 * self-signed or invalid SSL certificates. This setting only applies during the installation
	 * phase and does not impact the SSL validation of the installed profiler.
	 *
	 * @param insecure When set to true, SSL validation will be disabled during installation.
	 */
	@CommandLine.Option(
		names = ["--insecure"],
		description = [("""Disable SSL certificate validation during the installation, e.g. to accept self-signed or broken certificates.
This only affects the connection to Teamscale during the installation process, not the installed profiler.""")]
	)
	fun setInsecure(insecure: Boolean) {
		if (insecure) TeamscaleUtils.disableSslValidation()
	}

	/** Exit code for programming errors, i.e., bugs the user should report.  */
	const val EXIT_CODE_PROGRAMMING_ERROR = 3

	/** Exit code for permission errors, i.e., the user likely needs to run the installer as root.  */
	const val EXIT_CODE_PERMISSION_ERROR = 4

	/** Exit code for all other errors.  */
	const val EXIT_CODE_OTHER_ERROR = 5

	/** Entrypoint for the CLI.  */
	@JvmStatic
	fun main(args: Array<String>) {
		val exitCode = CommandLine(RootCommand).setCommandName(rootCommandName)
			.setCaseInsensitiveEnumValuesAllowed(true).execute(*args)
		exitProcess(exitCode)
	}

	private val rootCommandName: String
		get() {
			val rootCommandName = "installer"
			if (SystemUtils.IS_OS_WINDOWS) {
				return "$rootCommandName.exe"
			}
			return rootCommandName
		}
}
