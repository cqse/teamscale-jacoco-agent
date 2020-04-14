package com.teamscale.jacoco.agent.options;

import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Handles parsing a .txt file with classpath pattern separated by newlines. */
public class ClasspathUtils {

	/** Replaces all txt files in the given list with the file names written in the txt file separated by new lines. */
	public static List<File> resolveClasspathTextFiles(String key, FilePatternResolver filePatternResolver,
													   List<String> classDirectoriesOrZips) throws AgentOptionParseException {
		List<File> classDirectoriesOrZipFiles = new ArrayList<>();
		for (String classDirectoryOrZip : classDirectoriesOrZips) {
			classDirectoriesOrZipFiles.addAll(filePatternResolver.resolveToMultipleFiles(key, classDirectoryOrZip));
		}
		Map<Boolean, List<File>> filesByType = classDirectoriesOrZipFiles.stream()
				.collect(Collectors.partitioningBy(fileName -> fileName.getName().endsWith(".txt")));
		List<File> classJarOrDirFiles = new ArrayList<>(filesByType.get(false));
		for (File txtFile : filesByType.get(true)) {
			try {
				for (String file : FileSystemUtils.readLinesUTF8(txtFile)) {
					classJarOrDirFiles.addAll(filePatternResolver.resolveToMultipleFiles(key, file));
				}
			} catch (IOException e) {
				throw new AgentOptionParseException("Failed to resolve class path entries from the text files provided " +
						"in the `" + key + "` option. Please ensure that any text files provided in this option contain " +
						"properly formatted classpath pattern and each entry is separated by a newline.", e);
			}
		}
		return classJarOrDirFiles;
	}
}
