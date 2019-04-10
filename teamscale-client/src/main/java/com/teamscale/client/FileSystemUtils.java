/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright 2005-2011 The ConQAT Project                                   |
|                                                                          |
| Licensed under the Apache License, Version 2.0 (the "License");          |
| you may not use this file except in compliance with the License.         |
| You may obtain a copy of the License at                                  |
|                                                                          |
|    http://www.apache.org/licenses/LICENSE-2.0                            |
|                                                                          |
| Unless required by applicable law or agreed to in writing, software      |
| distributed under the License is distributed on an "AS IS" BASIS,        |
| WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. |
| See the License for the specific language governing permissions and      |
| limitations under the License.                                           |
+-------------------------------------------------------------------------*/
package com.teamscale.client;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * File system utilities.
 */
public class FileSystemUtils {

	/** Encoding for UTF-8. */
	public static final String UTF8_ENCODING = StandardCharsets.UTF_8.name();

	/** Unix file path separator */
	private static final char UNIX_SEPARATOR = '/';

	/**
	 * Checks if a directory exists. If not it creates the directory and all necessary parent directories.
	 *
	 * @throws IOException if directories couldn't be created.
	 */
	public static void ensureDirectoryExists(File directory) throws IOException {
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IOException("Couldn't create directory: " + directory);
		}
	}

	/**
	 * Returns a list of all files and directories contained in the given directory and all subdirectories matching the
	 * filter provided. The given directory itself is not included in the result.
	 * <p>
	 * The file filter may or may not exclude directories.
	 * <p>
	 * This method knows nothing about (symbolic and hard) links, so care should be taken when traversing directories
	 * containing recursive links.
	 *
	 * @param directory the directory to start the search from. If this is null or the directory does not exist, an
	 *                  empty list is returned.
	 * @param filter    the filter used to determine whether the result should be included. If the filter is null, all
	 *                  files and directories are included.
	 * @return the list of files found (the order is determined by the file system).
	 */
	public static List<File> listFilesRecursively(File directory, FileFilter filter) {
		if (directory == null || !directory.isDirectory()) {
			return Collections.emptyList();
		}
		List<File> result = new ArrayList<>();
		listFilesRecursively(directory, result, filter);
		return result;
	}

	/**
	 * Returns the extension of the file.
	 *
	 * @return File extension, i.e. "java" for "FileSystemUtils.java", or
	 * <code>null</code>, if the file has no extension (i.e. if a filename
	 * contains no '.'), returns the empty string if the '.' is the filename's last character.
	 */
	public static String getFileExtension(File file) {
		String name = file.getName();
		int posLastDot = name.lastIndexOf('.');
		if (posLastDot < 0) {
			return null;
		}
		return name.substring(posLastDot + 1);
	}

	/**
	 * Finds all files and directories contained in the given directory and all subdirectories matching the filter
	 * provided and put them into the result collection. The given directory itself is not included in the result.
	 * <p>
	 * This method knows nothing about (symbolic and hard) links, so care should be taken when traversing directories
	 * containing recursive links.
	 *
	 * @param directory the directory to start the search from.
	 * @param result    the collection to add to all files found.
	 * @param filter    the filter used to determine whether the result should be included. If the filter is null, all
	 *                  files and directories are included.
	 */
	private static void listFilesRecursively(File directory, Collection<File> result, FileFilter filter) {
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				listFilesRecursively(file, result, filter);
			}
			if (filter == null || filter.accept(file)) {
				result.add(file);
			}
		}
	}

	/**
	 * Replace platform dependent separator char with forward slashes to create system-independent paths.
	 */
	public static String normalizeSeparators(String path) {
		return path.replace(File.separatorChar, UNIX_SEPARATOR);
	}


}