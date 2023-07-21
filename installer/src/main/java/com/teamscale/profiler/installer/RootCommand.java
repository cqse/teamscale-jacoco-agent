package com.teamscale.profiler.installer;

import org.conqat.lib.commons.system.SystemUtils;
import picocli.CommandLine;

/**
 * The main command for the CLI.
 * Does nothing itself but hosts the subcommands.
 */
@CommandLine.Command(name = "installer",
		mixinStandardHelpOptions = true,
		usageHelpAutoWidth = true,
		scope = CommandLine.ScopeType.INHERIT,
		subcommands = {InstallCommand.class, UninstallCommand.class},
		description = "Installs or uninstalls the profiler system-wide. Must be run as root/Administrator.")
public class RootCommand {

	/** Exit code for programming errors, i.e. bugs the user should report. */
	public static final int EXIT_CODE_PROGRAMMING_ERROR = 3;

	/** Exit code for permission errors, i.e. the user likely needs to run the installer as root. */
	public static final int EXIT_CODE_PERMISSION_ERROR = 4;

	/** Exit code for all other errors. */
	public static final int EXIT_CODE_OTHER_ERROR = 5;

	@SuppressWarnings("InstantiationOfUtilityClass")
	private static final RootCommand INSTANCE = new RootCommand();

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
