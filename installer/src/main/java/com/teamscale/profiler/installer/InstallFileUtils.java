package com.teamscale.profiler.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InstallFileUtils {


	public static void makeReadable(Path path) throws FatalInstallerError {
		if (!path.toFile().setReadable(true)) {
			throw new FatalInstallerError(
					"Failed to make " + path + " readable. Please check file permissions.");
		}
	}

	public static void makeWritable(Path path) throws FatalInstallerError {
		if (!path.toFile().setWritable(true)) {
			throw new FatalInstallerError(
					"Failed to make " + path + " writable. Please check file permissions.");
		}
	}

	public static void createDirectory(Path directory) throws FatalInstallerError {
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new FatalInstallerError("Cannot create installation directory " + directory, e);
		}
	}

}
