package com.teamscale.profiler.installer;

import picocli.CommandLine;

import java.util.concurrent.Callable;

import static com.teamscale.profiler.installer.RootCommand.EXIT_CODE_PROGRAMMING_ERROR;

@CommandLine.Command(name = "uninstall", description = "Uninstalls the profiler.")
public class UninstallCommand implements Callable<Integer> {

	@Override
	public Integer call() {
		try {
			return Installer.uninstall();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.err.println("\n\nInstallation failed due to an internal error." +
					" This is likely a bug, please report the entire console output to support@teamscale.com");
			return EXIT_CODE_PROGRAMMING_ERROR;
		}
	}

}
