package com.teamscale.profiler.installer;

import okhttp3.HttpUrl;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.system.SystemUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Properties;
import java.util.stream.Stream;

public class Installer {

	private static final Path DEFAULT_SOURCE_DIRECTORY = Paths.get(".");
	private static final Path DEFAULT_INSTALL_DIRECTORY = Paths.get("/opt/teamscale-profiler/java");
	private static final Path DEFAULT_ETC_DIRECTORY = Paths.get("/etc");

	private final Path sourceDirectory;
	private final Path installDirectory;
	private final Path etcDirectory;

	public Installer(Path sourceDirectory, Path installDirectory, Path etcDirectory) {
		this.sourceDirectory = sourceDirectory;
		this.installDirectory = installDirectory;
		this.etcDirectory = etcDirectory;
	}

	public static void main(String[] args) {
		try {
			new Installer(DEFAULT_SOURCE_DIRECTORY, DEFAULT_INSTALL_DIRECTORY, DEFAULT_ETC_DIRECTORY).run(args);
			System.out.println("Installation successful. Profiler installed to " + DEFAULT_INSTALL_DIRECTORY);
			System.exit(0);
		} catch (FatalInstallerError e) {
			System.err.println("\n\nInstallation failed: " + e.getMessage());
			if (e.getCause() != null) {
				e.printStackTrace(System.err);
			}
			System.exit(1);
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.err.println("\n\nInstallation failed due to an internal error." +
					" This is likely a bug, please report the entire console output to support@teamscale.com");
			System.exit(2);
		}
	}

	public static class CommandlineUsageError extends FatalInstallerError {

		public CommandlineUsageError(String cause) {
			super(cause + "\n\nUSAGE: install-profiler [TEAMSCALE URL] [TEAMSCALE USER] [ACCESS KEY]");
		}

	}

	private Path getCoverageDirectory() {
		return installDirectory.resolve("coverage");
	}

	private Path getLogDirectory() {
		return installDirectory.resolve("logs");
	}

	private Path getTeamscalePropertiesPath() {
		return installDirectory.resolve("teamscale.properties");
	}

	private Path getAgentJarPath() {
		return installDirectory.resolve("lib/teamscale-jacoco-agent.jar");
	}

	// TODO (FS) add uninstall
	public void run(String[] args) throws FatalInstallerError {
		TeamscaleCredentials credentials = parseCredentials(args);
		TeamscaleUtils.checkTeamscaleConnection(credentials);
		createAgentDirectory();
		copyAgentFiles();
		writeTeamscaleProperties(credentials);
		makeCoverageAndLogDirectoriesWorldWritable();
		makeAllProfilerFilesWorldReadable();
		enableSystemWide();
	}

	private void enableSystemWide() throws FatalInstallerError {
		if (SystemUtils.isLinux()) {
			addToEtcEnvironment();
			addToSystemdDefaultEnvironment();
		}
		// TODO (FS) windows
	}

	/**
	 * Returns the environment variables to set system-wide to register the agent. We currently set two options:
	 * <ul>
	 * <li>JAVA_TOOL_OPTIONS is recognized by all JVMs but may be overridden by application start scripts
	 * <li>_JAVA_OPTIONS is not officially documented but currently well-supported and unlikely to be used
	 * by application start scripts
	 * </ul>
	 */
	private EnvironmentMap getEnvironmentVariables() {
		String javaAgentArgument = "-javaagent:" + getAgentJarPath();
		return new EnvironmentMap("JAVA_TOOL_OPTIONS", javaAgentArgument,
				"_JAVA_OPTIONS", javaAgentArgument);
	}

	private void addToEtcEnvironment() throws FatalInstallerError {
		Path environmentFile = etcDirectory.resolve("environment");
		if (!Files.exists(environmentFile)) {
			System.out.println(
					environmentFile + " does not exist. Skipping global registration of the profiler there.");
			return;
		}

		String content = "\n" + getEnvironmentVariables().getEtcEnvironmentString();

		try {
			Files.write(environmentFile, content.getBytes(StandardCharsets.US_ASCII),
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new FatalInstallerError("Could not change contents of " + environmentFile, e);
		}
	}

	private void addToSystemdDefaultEnvironment() throws FatalInstallerError {
		Path systemdEtcDirectory = etcDirectory.resolve("systemd");
		if (!Files.exists(systemdEtcDirectory)) {
			return;
		}

		Path systemdConfig = systemdEtcDirectory.resolve("teamscale-profiler.conf");
		if (Files.exists(systemdConfig)) {
			throw new FatalInstallerError("Cannot create systemd configuration file " + systemdConfig);
		}

		String content = "[Manager]\nDefaultEnvironment=" + getEnvironmentVariables().getSystemdString() + "\n";
		try {
			Files.write(systemdConfig, content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FatalInstallerError("Could not create " + systemdConfig, e);
		}
	}

	private void makeAllProfilerFilesWorldReadable() throws FatalInstallerError {
		try (Stream<Path> fileStream = Files.walk(installDirectory)) {
			for (Iterator<Path> it = fileStream.iterator(); it.hasNext(); ) {
				Path path = it.next();
				InstallFileUtils.makeReadable(path);
			}
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to list all files in " + installDirectory + ".", e);
		}
	}

	private void makeCoverageAndLogDirectoriesWorldWritable() throws FatalInstallerError {
		InstallFileUtils.createDirectory(getCoverageDirectory());
		InstallFileUtils.makeWritable(getCoverageDirectory());

		InstallFileUtils.createDirectory(getLogDirectory());
		InstallFileUtils.makeWritable(getLogDirectory());

	}

	private void writeTeamscaleProperties(TeamscaleCredentials credentials) throws FatalInstallerError {
		Properties properties = new Properties();
		properties.setProperty("url", credentials.url.toString());
		properties.setProperty("username", credentials.username);
		properties.setProperty("accesskey", credentials.accessKey);

		try (OutputStream out = Files.newOutputStream(getTeamscalePropertiesPath(), StandardOpenOption.WRITE,
				StandardOpenOption.CREATE)) {
			properties.store(out, null);
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to write " + getTeamscalePropertiesPath() + ".", e);
		}

		InstallFileUtils.makeReadable(getTeamscalePropertiesPath());
	}

	private void copyAgentFiles() throws FatalInstallerError {
		try {
			FileSystemUtils.copyFiles(sourceDirectory.toFile(), installDirectory.toFile(), null);
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to copy some files to " + installDirectory + "."
					+ " Please manually clean up " + installDirectory, e);
		}
	}

	private void createAgentDirectory() throws FatalInstallerError {
		if (Files.exists(installDirectory)) {
			throw new FatalInstallerError("Cannot install to " + installDirectory + ": Path already exists");
		}

		InstallFileUtils.createDirectory(installDirectory);
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
