package com.teamscale.profiler.installer;

import com.teamscale.profiler.installer.steps.IStep;
import com.teamscale.profiler.installer.steps.InstallAgentFilesStep;
import com.teamscale.profiler.installer.steps.InstallEtcEnvironmentStep;
import com.teamscale.profiler.installer.steps.InstallSystemdStep;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.system.SystemUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** Installs the agent system-globally. */
public class Installer {

	private static final Path DEFAULT_INSTALL_DIRECTORY = Paths.get("/opt/teamscale-profiler/java");
	private static final Path DEFAULT_ETC_DIRECTORY = Paths.get("/etc");

	private static final String RERUN_ADVICE = getRerunAdvice();

	private static String getRerunAdvice() {
		if (SystemUtils.isWindows()) {
			return "Try running this installer as Administrator.";
		} else {
			return "Try running this installer as root, e.g. with sudo.";
		}
	}


	/** Returns the directory that contains the agent to install or null if it can't be resolved. */
	private static Path getDefaultSourceDirectory() {
		try {
			URI jarFileUri = Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			// we assume that the dist zip is extracted and the installer jar not moved
			return Paths.get(jarFileUri).getParent();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to obtain agent directory. This is a bug, please report it.", e);
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
	public Installer(Path sourceDirectory, Path installDirectory, Path etcDirectory) {
		EnvironmentMap environmentVariables = getEnvironmentVariables(installDirectory);
		this.steps = Arrays.asList(new InstallAgentFilesStep(sourceDirectory, installDirectory),
				new InstallEtcEnvironmentStep(etcDirectory, environmentVariables),
				new InstallSystemdStep(etcDirectory, environmentVariables));
	}

	/**
	 * Runs the installer. Expected command-line arguments: [TEAMSCALE URL] [TEAMSCALE USER] [ACCESS KEY] Alternatively,
	 * running with --uninstall will uninstall the profiler.
	 */
	public static void main(String[] args) {
		Installer installer = new Installer(getDefaultSourceDirectory(), DEFAULT_INSTALL_DIRECTORY, DEFAULT_ETC_DIRECTORY);

		try {
			if (args.length == 1 && args[0].equals("--uninstall")) {
				uninstall(installer);
			} else {
				install(installer, args);
			}

			// we use System.exit here to make sure that no background threads spawned by libraries we use prevent the
			// program from exiting. May e.g. happen with okhttp
			System.exit(0);
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.err.println("\n\nInstallation failed due to an internal error." +
					" This is likely a bug, please report the entire console output to support@teamscale.com");
			System.exit(2);
		}
	}

	private static void install(Installer installer, String[] args) {
		try {
			installer.install(args);
			System.out.println("Installation successful. Profiler installed to " + DEFAULT_INSTALL_DIRECTORY);
			System.out.println("To activate the profiler for an application, set the environment variable"
					+ "\nTEAMSCALE_JAVA_PROFILER_CONFIG"
					+ "\nIts value must be the path to a valid profiler configuration file."
					+ "\nThen, restart your application (for web applications: restart the app server).");
		} catch (PermissionError e) {
			e.printToStderr();

			System.err.println(
					"\n\nInstallation failed because the installer had insufficient permissions to make the necessary"
							+ " changes on your system.\nSee above for error messages.\n" + RERUN_ADVICE);
			System.exit(1);
		} catch (FatalInstallerError e) {
			e.printToStderr();
			System.err.println("\n\nInstallation failed. See above for error messages.");
			System.exit(2);
		}
	}

	private static void uninstall(Installer installer) {
		UninstallerErrorReporter errorReporter = installer.uninstall();
		if (errorReporter.errorsReported) {
			String message = "Uninstallation failed. See above for error messages.";
			if (errorReporter.hadPermissionError) {
				message += "\n" + RERUN_ADVICE;
			}
			System.err.println("\n\n" + message);
			System.exit(1);
		}
		System.out.println("Profiler successfully uninstalled");
	}

	/**
	 * Thrown when the users command-line parameters are invalid. Includes a helpful message how to supply correct
	 * command-line arguments.
	 */
	public static class CommandlineUsageError extends FatalInstallerError {

		public CommandlineUsageError(String cause) {
			super(cause + "\n\nUSAGE: install-profiler [TEAMSCALE URL] [TEAMSCALE USER] [ACCESS KEY]");
		}

	}

	/**
	 * Installs the profiler.
	 *
	 * @throws FatalInstallerError if a step of the installation process fails.
	 */
	public void install(String[] args) throws FatalInstallerError {
		TeamscaleCredentials credentials = parseCredentials(args);
		TeamscaleUtils.checkTeamscaleConnection(credentials);
		install(credentials);
		System.out.println("Installation successful. Profiler installed to " + DEFAULT_INSTALL_DIRECTORY);
	}

	private void install(TeamscaleCredentials credentials) throws FatalInstallerError {
		for (IStep step : steps) {
			step.install(credentials);
		}
	}

	/**
	 * Uninstalls the profiler. All errors that happened during the uninstallation are reported via the returned
	 * {@link UninstallerErrorReporter}.
	 */
	public UninstallerErrorReporter uninstall() {
		UninstallerErrorReporter errorReporter = new UninstallerErrorReporter();
		for (IStep step : CollectionUtils.reverse(steps)) {
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

	private TeamscaleCredentials parseCredentials(String[] args) throws FatalInstallerError {
		if (args.length < 3) {
			throw new CommandlineUsageError("You must provide 3 command line arguments");
		}

		String urlArgument = args[0];
		HttpUrl url = HttpUrl.parse(urlArgument);
		if (url == null) {
			throw new CommandlineUsageError("This is not a valid URL: " + urlArgument);
		}

		return new TeamscaleCredentials(url, args[1], args[2]);
	}

}
