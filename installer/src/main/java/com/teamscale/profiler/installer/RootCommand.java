package com.teamscale.profiler.installer;

import org.conqat.lib.commons.system.SystemUtils;
import picocli.CommandLine;

/**
 * The main command for the CLI. Does nothing itself but hosts the subcommands.
 */
@CommandLine.Command(name = "installer",
		mixinStandardHelpOptions = true,
		usageHelpAutoWidth = true,
		scope = CommandLine.ScopeType.INHERIT,
		subcommands = {InstallCommand.class, UninstallCommand.class},
		versionProvider = VersionProvider.class,
		description = "Installs or uninstalls the profiler system-wide. Must be run as root/Administrator.")
public class RootCommand {

	/** Exit code for programming errors, i.e. bugs the user should report. */
	public static final int EXIT_CODE_PROGRAMMING_ERROR = 3;

	/** Exit code for permission errors, i.e. the user likely needs to run the installer as root. */
	public static final int EXIT_CODE_PERMISSION_ERROR = 4;

	/** Exit code for all other errors. */
	public static final int EXIT_CODE_OTHER_ERROR = 5;

	private static final RootCommand INSTANCE = new RootCommand();

	@CommandLine.Option(names = "--insecure", description = "Disable SSL certificate validation during the installation,"
			+ " e.g. to accept self-signed or broken certificates."
			+ "\nThis only affects the connection to Teamscale during the installation process, not the installed profiler.")
	public void setInsecure(boolean insecure) {
		if (insecure) {
			TeamscaleUtils.disableSslValidation();
		}
	}

	/** Entrypoint for the CLI. */
	public static void main(String[] args) {
		int exitCode = new CommandLine(INSTANCE).setCommandName(getRootCommandName())
				.setCaseInsensitiveEnumValuesAllowed(true).execute(args);
		System.exit(exitCode);
	}

	private static String getRootCommandName() {
		String rootCommandName = "installer";
		if (SystemUtils.isWindows()) {
			return rootCommandName.concat(".exe");
		}
		return rootCommandName;
	}


}
