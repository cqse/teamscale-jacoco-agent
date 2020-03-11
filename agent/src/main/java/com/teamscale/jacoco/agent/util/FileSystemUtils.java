package com.teamscale.jacoco.agent.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemUtils {
	/**
	 * Delete a directory from disk if it is empty. This method does nothing if the path provided does not exist or
	 * point to a file.
	 *
	 * @throws IOException if the deletion of the directory fails
	 */
	public static void deleteDirectoryIfEmpty(Path directory) throws IOException {
		if (Files.isDirectory(directory) && Files.list(directory).toArray().length == 0) {
			Files.delete(directory);
		}
	}
}
