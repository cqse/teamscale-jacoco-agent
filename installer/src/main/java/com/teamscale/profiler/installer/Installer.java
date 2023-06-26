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

	private static final Path SOURCE_DIRECTORY = Paths.get(".");
	private static final Path INSTALL_DIRECTORY = Paths.get("/opt/teamscale-profiler/java");
	private static final Path COVERAGE_DIRECTORY = INSTALL_DIRECTORY.resolve("coverage");
	private static final Path LOG_DIRECTORY = INSTALL_DIRECTORY.resolve("logs");
	private static final Path TEAMSCALE_PROPERTIES_PATH = INSTALL_DIRECTORY.resolve("teamscale.properties");

	public static void main(String[] args) {
		new Installer().run(args);
	}

	private static class CommandlineUsageError extends FatalInstallerError {

		public CommandlineUsageError(String cause) {
			super(cause + "\n\nUSAGE: install-profiler [TEAMSCALE URL] [TEAMSCALE USER] [ACCESS KEY]");
		}

	}

	public void run(String[] args) {
		try {
			TeamscaleCredentials credentials = parseCredentials(args);
			TeamscaleUtils.checkTeamscaleConnection(credentials);
			createAgentDirectory();
			copyAgentFiles();
			writeTeamscaleProperties(credentials);
			makeCoverageAndLogDirectoriesWorldWritable();
			makeAllProfilerFilesWorldReadable();
			enableSystemWide();
			printSuccessMessage();
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

	private void printSuccessMessage() {
		System.out.println("Installation successful. Profiler installed to ???"); // TODO (FS)
	}

	private void enableSystemWide() {
	}

	private void makeAllProfilerFilesWorldReadable() throws FatalInstallerError {
		try (Stream<Path> fileStream = Files.walk(INSTALL_DIRECTORY)) {
			for (Iterator<Path> it = fileStream.iterator(); it.hasNext(); ) {
				Path path = it.next();
				InstallFileUtils.makeReadable(path);
			}
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to list all files in " + INSTALL_DIRECTORY + ".", e);
		}
	}

	private void makeCoverageAndLogDirectoriesWorldWritable() throws FatalInstallerError {
		InstallFileUtils.createDirectory(COVERAGE_DIRECTORY);
		InstallFileUtils.makeWritable(COVERAGE_DIRECTORY);

		InstallFileUtils.createDirectory(LOG_DIRECTORY);
		InstallFileUtils.makeWritable(LOG_DIRECTORY);

	}

	private void writeTeamscaleProperties(TeamscaleCredentials credentials) throws FatalInstallerError {
		Properties properties = new Properties();
		properties.setProperty("url", credentials.url.toString());
		properties.setProperty("username", credentials.username);
		properties.setProperty("accesskey", credentials.accessKey);

		try (OutputStream out = Files.newOutputStream(TEAMSCALE_PROPERTIES_PATH, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE)) {
			properties.store(out, null);
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to write " + TEAMSCALE_PROPERTIES_PATH + ".", e);
		}
	}

	private void copyAgentFiles() throws FatalInstallerError {
		try {
			FileSystemUtils.copyFiles(SOURCE_DIRECTORY.toFile(), INSTALL_DIRECTORY.toFile(), null);
		} catch (IOException e) {
			throw new FatalInstallerError("Failed to copy some files to " + INSTALL_DIRECTORY + "."
					+ " Please manually clean up " + INSTALL_DIRECTORY, e);
		}
	}

	private void createAgentDirectory() throws FatalInstallerError {
		if (Files.exists(INSTALL_DIRECTORY)) {
			throw new FatalInstallerError("Cannot install to " + INSTALL_DIRECTORY + ": Path already exists");
		}

		InstallFileUtils.createDirectory(INSTALL_DIRECTORY);
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
