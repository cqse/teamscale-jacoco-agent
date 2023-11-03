package com.teamscale.profiler.installer.steps;

import com.teamscale.profiler.installer.EnvironmentMap;
import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.PermissionError;
import com.teamscale.profiler.installer.TeamscaleCredentials;
import org.conqat.lib.commons.system.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * On Linux with systemd, registers the agent globally for systemd services. This is necessary in addition to
 * {@link InstallEtcEnvironmentStep}, since systemd doesn't always inject /etc/environment into started services.
 */
public class InstallSystemdStep implements IStep {

	private final Path etcDirectory;
	private final EnvironmentMap environmentVariables;
	private final boolean reloadSystemdDaemon;

	public InstallSystemdStep(Path etcDirectory, EnvironmentMap environmentMap, boolean reloadSystemdDaemon) {
		this.etcDirectory = etcDirectory;
		this.environmentVariables = environmentMap;
		this.reloadSystemdDaemon = reloadSystemdDaemon;
	}

	@Override
	public void install(TeamscaleCredentials credentials) throws FatalInstallerError {
		if (!SystemUtils.isLinux()) {
			return;
		}

		if (!Files.exists(getSystemdEtcDirectory())) {
			System.out.println("systemd could not be detected. Not installing profiler for systemd services.");
			// system has no systemd installed
			return;
		}

		if (!Files.exists(getSystemdSystemConfDDirectory())) {
			try {
				Files.createDirectories(getSystemdSystemConfDDirectory());
			} catch (IOException e) {
				throw new PermissionError("Cannot create system.conf.d directory: " + getSystemdSystemConfDDirectory(),
						e);
			}
		}

		Path systemdConfigFile = getSystemdConfigFile();
		if (Files.exists(systemdConfigFile)) {
			throw new PermissionError(
					"Cannot create systemd configuration file " + systemdConfigFile + " because it already exists." +
							"\nPlease uninstall any old profiler versions first");
		}

		String content = "[Manager]\nDefaultEnvironment=" + environmentVariables.getSystemdString() + "\n";
		try {
			Files.write(systemdConfigFile, content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new PermissionError("Could not create " + systemdConfigFile, e);
		}

		daemonReload();
	}

	private void daemonReload() {
		if (!this.reloadSystemdDaemon) {
			return;
		}

		try {
			ProcessBuilder builder = new ProcessBuilder("systemctl", "daemon-reload")
					// must redirect program output or it might hang forever
					.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null")))
					.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/null")));

			Process process = builder.start();
			if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
				// timeout
				process.destroyForcibly();
				askUserToManuallyReloadDaemon();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			askUserToManuallyReloadDaemon();
		}
	}

	private void askUserToManuallyReloadDaemon() {
		System.err.println(
				"Failed to reload the systemd daemon. Systemd services can only be profiled after reloading the daemon." +
						"\nPlease manually reload the daemon with:" +
						"\nsystemctl daemon-reload");
	}

	private Path getSystemdEtcDirectory() {
		return etcDirectory.resolve("systemd");
	}

	private Path getSystemdSystemConfDDirectory() {
		return getSystemdEtcDirectory().resolve("system.conf.d");
	}

	private Path getSystemdConfigFile() {
		return getSystemdSystemConfDDirectory().resolve("teamscale-java-profiler.conf");
	}

	@Override
	public void uninstall(IUninstallErrorReporter errorReporter) {
		if (!SystemUtils.isLinux()) {
			return;
		}

		Path systemdConfigFile = getSystemdConfigFile();
		if (!Files.exists(systemdConfigFile)) {
			return;
		}

		try {
			Files.delete(systemdConfigFile);
		} catch (IOException e) {
			errorReporter.report(
					new PermissionError("Failed to remove systemd config file " + systemdConfigFile + "." +
							" Manually remove this file or systemd Java services may fail to start.", e));
		}

		daemonReload();
	}
}
