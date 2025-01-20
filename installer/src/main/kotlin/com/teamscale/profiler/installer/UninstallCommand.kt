package com.teamscale.profiler.installer

import picocli.CommandLine
import java.util.concurrent.Callable

/**
 * CLI command that uninstalls the profiler.
 */
@CommandLine.Command(name = "uninstall", description = ["Uninstalls the profiler."])
class UninstallCommand : Callable<Int> {
	override fun call(): Int {
		try {
			return Installer.uninstall()
		} catch (t: Throwable) {
			t.printStackTrace(System.err)
			System.err.println(
				"""
				
				
				Installation failed due to an internal error. This is likely a bug, please report the entire console output to support@teamscale.com
				""".trimIndent()
			)
			return RootCommand.EXIT_CODE_PROGRAMMING_ERROR
		}
	}
}
