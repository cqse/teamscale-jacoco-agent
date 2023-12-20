package com.teamscale.profiler.installer.utils;

import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.PermissionError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utilities for manipulating files during the installation process. */
public class InstallFileUtils {

	/** Makes the given path world-readable. */
	public static void makeReadable(Path path) throws FatalInstallerError {
		if (!path.toFile().setReadable(true, false)) {
			throw new PermissionError(
					"Failed to make " + path + " readable. Please check file permissions.");
		}
	}

	/** Creates the given directory, handling errors. */
	public static void createDirectory(Path directory) throws FatalInstallerError {
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new PermissionError("Cannot create directory " + directory, e);
		}
	}

}
