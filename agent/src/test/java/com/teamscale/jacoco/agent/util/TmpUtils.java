package com.teamscale.jacoco.agent.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TmpUtils {
	/**
	 * Removes all "jacoco-*.xml" files from the system tmp dir
	 */
	public static void cleanTmpFolder() throws IOException {
		Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
		Files.list(tmpDir)
				.filter(path -> {
					String fileName = path.getFileName().toString();
					return fileName.startsWith("jacoco-") && fileName.endsWith(".xml");
				})
				.forEach(path -> path.toFile().delete());
	}
}
