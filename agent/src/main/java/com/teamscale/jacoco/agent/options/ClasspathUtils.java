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
													   List<String> patterns) throws AgentOptionParseException {
		List<File> resolvedPaths = new ArrayList<>();
		for (String pattern : patterns) {
			resolvedPaths.addAll(filePatternResolver.resolveToMultipleFiles(key, pattern));
		}
		Map<Boolean, List<File>> filesByType = resolvedPaths.stream()
				.collect(Collectors.partitioningBy(file -> file.getName().endsWith(".txt")));
		List<File> classDirOrJarFiles = new ArrayList<>(filesByType.get(false));
		for (File txtFile : filesByType.get(true)) {
			classDirOrJarFiles.addAll(resolveClassPathEntries(key, filePatternResolver, txtFile));
		}
		return classDirOrJarFiles;
	}

	private static List<File> resolveClassPathEntries(String key, FilePatternResolver filePatternResolver,
													  File txtFile) throws AgentOptionParseException {
		try {
			List<String> filePaths = FileSystemUtils.readLinesUTF8(txtFile);
			List<File> resolvedFiles = new ArrayList<>();
			for (String filePath : filePaths) {
				resolvedFiles.addAll(filePatternResolver.resolveToMultipleFiles(key, filePath));
			}
			return resolvedFiles;
		} catch (IOException e) {
			throw new AgentOptionParseException("Failed to resolve class path entries from the text files provided " +
					"in the `" + key + "` option. Please ensure that any text files provided in this option contain " +
					"properly formatted classpath patterns and each entry is separated by a newline.", e);
		}
	}
}
