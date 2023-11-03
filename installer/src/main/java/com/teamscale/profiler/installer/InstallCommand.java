package com.teamscale.profiler.installer;

import okhttp3.HttpUrl;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static com.teamscale.profiler.installer.RootCommand.EXIT_CODE_PROGRAMMING_ERROR;

/**
 * CLI command that installs the profiler.
 */
@CommandLine.Command(name = "install", description = "Installs the profiler.", versionProvider = VersionProvider.class)
public class InstallCommand implements Callable<Integer> {

	@CommandLine.Parameters(index = "0", converter = HttpUrlTypeConverter.class, description = "The URL of your Teamscale instance.")
	private HttpUrl teamscaleUrl;

	@CommandLine.Parameters(index = "1", description = "The user used to access your Teamscale instance and upload coverage to your projects.")
	private String userName;

	@CommandLine.Parameters(index = "2", description = "The access key of the given user. NOT the password!")
	private String accessKey;

	@Override
	public Integer call() {
		try {
			return Installer.install(new TeamscaleCredentials(teamscaleUrl, userName, accessKey));
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.err.println("\n\nInstallation failed due to an internal error." +
					" This is likely a bug, please report the entire console output to support@teamscale.com");
			return EXIT_CODE_PROGRAMMING_ERROR;
		}
	}

}
