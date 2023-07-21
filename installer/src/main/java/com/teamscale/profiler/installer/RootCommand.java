package com.teamscale.profiler.installer;

import org.conqat.lib.commons.system.SystemUtils;
import picocli.CommandLine;

@CommandLine.Command(name = "installer",
		mixinStandardHelpOptions = true,
		usageHelpAutoWidth = true,
		scope = CommandLine.ScopeType.INHERIT,
		subcommands = {InstallCommand.class, UninstallCommand.class},
		description = "Installs or uninstalls the profiler system-wide. Must be run as root/Administrator.")
public class RootCommand {

	public static final int EXIT_CODE_PROGRAMMING_ERROR = 3;
	public static final int EXIT_CODE_PERMISSION_ERROR = 4;
	public static final int EXIT_CODE_OTHER_ERROR = 5;

	@SuppressWarnings("InstantiationOfUtilityClass")
	private static final RootCommand INSTANCE = new RootCommand();

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
