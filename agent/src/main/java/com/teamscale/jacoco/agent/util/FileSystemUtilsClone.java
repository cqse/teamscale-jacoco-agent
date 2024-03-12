package com.teamscale.jacoco.agent.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Clone of the file system utilities.
 * <p>
 * This shall be removed with TS-37964.
 */
public class FileSystemUtilsClone {
	
	/**
	 * Clone of {@link com.teamscale.client.FileSystemUtils#listFilesRecursively(File, FileFilter)}
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
	 * Clone of {@link com.teamscale.client.FileSystemUtils#listFilesRecursively(File, Collection, FileFilter)} 
	 */
	private static void listFilesRecursively(File directory, Collection<File> result, FileFilter filter) {
		File[] files = directory.listFiles();
		if (files == null) {
			// From the docs of `listFiles`:
			// 		"If this abstract pathname does not denote a directory, then this method returns null."
			// Based on this, it seems to be ok to just return here without throwing an exception.
			return;
		}

		for (File file : files) {
			if (file.isDirectory()) {
				listFilesRecursively(file, result, filter);
			}
			if (filter == null || filter.accept(file)) {
				result.add(file);
			}
		}
	}

}