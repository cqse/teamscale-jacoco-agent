package com.teamscale.profiler.installer;

import com.teamscale.profiler.installer.steps.IStep;
import com.teamscale.profiler.installer.steps.InstallAgentFilesStep;
import com.teamscale.profiler.installer.steps.InstallEtcEnvironmentStep;
import com.teamscale.profiler.installer.steps.InstallSystemdStep;
import com.teamscale.profiler.installer.steps.InstallWindowsSystemEnvironmentStep;
import com.teamscale.profiler.installer.windows.WindowsRegistry;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.system.SystemUtils;
import picocli.CommandLine;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** Installs the agent system-globally. */
public class Installer {

	private static final Path DEFAULT_INSTALL_DIRECTORY = windowsOrLinux(
			Paths.get(System.getenv("ProgramFiles")),
			Paths.get("/opt/teamscale-profiler/java")
	);

	private static final Path DEFAULT_ETC_DIRECTORY = Paths.get("/etc");

	private static final String RERUN_ADVICE = windowsOrLinux(
			"Try running this installer as Administrator.",
			"Try running this installer as root, e.g. with sudo."
	);

	private static <T> T windowsOrLinux(T windowsValue, T linuxValue) {
		if (SystemUtils.isWindows()) {
			return windowsValue;
		} else {
			return linuxValue;
		}
	}

	/** Returns the directory that contains the agent to install or null if it can't be resolved. */
	private static Path getDefaultSourceDirectory() {
		try {
			URI jarFileUri = Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			// we assume that the dist zip is extracted and the installer jar not moved
			return Paths.get(jarFileUri).getParent();
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Failed to obtain agent directory. This is a bug, please report it.", e);
		}
	}

	/**
	 * List of steps to run during installation and uninstallation. During uninstallation, steps are run in the reverse
	 * order of this list. After each step, the system must be in a safe state, meaning the user's applications must
	 * still run. This is especially important during uninstallation.
	 */
	private final List<IStep> steps;

	/**
	 * Constructor. Making the following directories configurable allows testing the installer without admin
	 * permissions.
	 *
	 * @param sourceDirectory  directory that contains the profiler binaries and support files to install.
	 * @param installDirectory directory to which to install the profiler.
	 * @param etcDirectory     on Linux: the /etc directory
	 */
	public Installer(Path sourceDirectory, Path installDirectory, Path etcDirectory, boolean reloadSystemdDaemon) {
		EnvironmentMap environmentVariables = getEnvironmentVariables(installDirectory);
		this.steps = Arrays.asList(new InstallAgentFilesStep(sourceDirectory, installDirectory),
				new InstallWindowsSystemEnvironmentStep(environmentVariables, WindowsRegistry.INSTANCE),
				new InstallEtcEnvironmentStep(etcDirectory, environmentVariables),
				new InstallSystemdStep(etcDirectory, environmentVariables, reloadSystemdDaemon));
	}

	/**
	 * Installs the profiler with the given Teamscale credentials.
	 *
	 * @return the exit code for the CLI.
	 */
	public static int install(TeamscaleCredentials credentials) {
		try {
			getDefaultInstaller().runInstall(credentials);
			System.out.println("Installation successful. Profiler installed to " + DEFAULT_INSTALL_DIRECTORY);
			if (SystemUtils.isLinux()) {
				System.out.println("To use the profiler in your current session, please log out and back in.");
			}
			System.out.println("To activate the profiler for an application, set the environment variable:"
					+ "\nTEAMSCALE_JAVA_PROFILER_CONFIG_ID"
					+ "\nIts value must be a valid profiler configuration ID defined in the Teamscale instance."
					+ "\nThen, restart your application (for web applications: restart the app server)."
					+ "\nIn an interactive session, you have to log out and log back in for the changes to take effect.");
			return CommandLine.ExitCode.OK;
		} catch (PermissionError e) {
			e.printToStderr();

			System.err.println(
					"\n\nInstallation failed because the installer had insufficient permissions to make the necessary"
							+ " changes on your system.\nSee above for error messages.\n" + RERUN_ADVICE);
			return RootCommand.EXIT_CODE_PERMISSION_ERROR;
		} catch (FatalInstallerError e) {
			e.printToStderr();
			System.err.println("\n\nInstallation failed. See above for error messages.");
			return RootCommand.EXIT_CODE_OTHER_ERROR;
		}
	}

	private static Installer getDefaultInstaller() {
		return new Installer(getDefaultSourceDirectory(), DEFAULT_INSTALL_DIRECTORY, DEFAULT_ETC_DIRECTORY, true);
	}


	/**
	 * Uninstalls the profiler.
	 *
	 * @return the exit code for the CLI.
	 */
	public static int uninstall() {
		UninstallerErrorReporter errorReporter = getDefaultInstaller().runUninstall();
		if (errorReporter.errorsReported) {
			String message = "Uninstallation failed. See above for error messages.";
			if (errorReporter.hadPermissionError) {
				message += "\n" + RERUN_ADVICE;
			}
			System.err.println("\n\n" + message);
			return RootCommand.EXIT_CODE_OTHER_ERROR;
		}
		System.out.println("Profiler successfully uninstalled.\n" +
				"You need to restart all previously profiled applications to stop profiling them.");
		return CommandLine.ExitCode.OK;
	}

	/**
	 * Installs the profiler.
	 *
	 * @throws FatalInstallerError if a step of the installation process fails.
	 */
	public void runInstall(TeamscaleCredentials credentials) throws FatalInstallerError {
		TeamscaleUtils.checkTeamscaleConnection(credentials);
		for (IStep step : steps) {
			if (!step.shouldRun()) {
				continue;
			}
			step.install(credentials);
		}
	}

	/**
	 * Uninstalls the profiler. All errors that happened during the uninstallation are reported via the returned
	 * {@link UninstallerErrorReporter}.
	 */
	public UninstallerErrorReporter runUninstall() {
		UninstallerErrorReporter errorReporter = new UninstallerErrorReporter();
		for (IStep step : CollectionUtils.reverse(steps)) {
			if (!step.shouldRun()) {
				continue;
			}

			step.uninstall(errorReporter);
			if (errorReporter.errorsReported) {
				break;
			}
		}
		return errorReporter;
	}

	/**
	 * Reports errors during installation to stderr.
	 */
	public static class UninstallerErrorReporter implements IStep.IUninstallErrorReporter {

		private boolean errorsReported = false;
		private boolean hadPermissionError = false;

		/** Whether at least one error was reported. */
		public boolean wereErrorsReported() {
			return errorsReported;
		}

		@Override
		public void report(FatalInstallerError e) {
			errorsReported = true;
			if (e instanceof PermissionError) {
				hadPermissionError = true;
			}
			e.printToStderr();
		}
	}

	/**
	 * Returns the environment variables to set system-wide to register the agent. We currently set two options:
	 * <ul>
	 * <li>JAVA_TOOL_OPTIONS is recognized by all JVMs but may be overridden by application start scripts
	 * <li>_JAVA_OPTIONS is not officially documented but currently well-supported and unlikely to be used
	 * by application start scripts
	 * </ul>
	 */
	private EnvironmentMap getEnvironmentVariables(Path installDirectory) {
		String javaAgentArgument = "-javaagent:" + getAgentJarPath(installDirectory);
		return new EnvironmentMap("JAVA_TOOL_OPTIONS", javaAgentArgument,
				"_JAVA_OPTIONS", javaAgentArgument);
	}

	private Path getAgentJarPath(Path installDirectory) {
		return installDirectory.resolve("lib/teamscale-jacoco-agent.jar");
	}

}
