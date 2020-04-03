package com.teamscale.jacoco.agent.util;

import com.teamscale.jacoco.agent.options.AgentOptionParseException;
import com.teamscale.jacoco.agent.options.FilePatternResolver;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Handles parsing a .txt file with a classpath. */
public class ClasspathUtils {
	/** Replaces all txt files in the given list with the file names written in the txt file separated by new lines. */
	public static List<File> resolveClasspathTextFiles(String key, FilePatternResolver filePatternResolver,
													   List<String> classDirectoriesOrZips) throws AgentOptionParseException, IOException {
		List<File> classDirectoriesOrZipFiles = CollectionUtils
				.mapWithException(classDirectoriesOrZips, file -> filePatternResolver.parseFile(key, file));
		Map<Boolean, List<File>> filesByType = classDirectoriesOrZipFiles.stream()
				.collect(Collectors.partitioningBy(fileName -> fileName.getName().endsWith(".txt")));
		List<File> classJarOrDirFiles = new ArrayList<>(filesByType.get(false));
		for (File txtFile : filesByType.get(true)) {
			try {
				classJarOrDirFiles.addAll(CollectionUtils.mapWithException(FileSystemUtils.readLinesUTF8(txtFile),
						file -> filePatternResolver.parseFile(key, file)));
			} catch (IOException e) {
				throw new IOException("Failed to resolve classpath entries in " + txtFile, e);
			}
		}
		return classJarOrDirFiles;
	}
}
