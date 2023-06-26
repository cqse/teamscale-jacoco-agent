package com.teamscale.profiler.installer;

import okhttp3.HttpUrl;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.IOException;
import java.io.OutputStream;
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

	private final Path sourceDirectory;
	private final Path installDirectory;

	public Installer(Path sourceDirectory, Path installDirectory) {
		this.sourceDirectory = sourceDirectory;
		this.installDirectory = installDirectory;
	}

	public static void main(String[] args) {
		try {
			new Installer(DEFAULT_SOURCE_DIRECTORY, DEFAULT_INSTALL_DIRECTORY).run(args);
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

	private static class CommandlineUsageError extends FatalInstallerError {

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

	private void enableSystemWide() {
		// TODO (FS)
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
