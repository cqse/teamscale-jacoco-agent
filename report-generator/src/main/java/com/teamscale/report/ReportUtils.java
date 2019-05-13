package com.teamscale.report;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.teamscale.client.FileSystemUtils;
import com.teamscale.report.testwise.ETestArtifactFormat;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

/** Utilities for generating reports. */
public class ReportUtils {

	private static Moshi moshi = new Moshi.Builder().build();

	/** Converts to given testwise coverage report to a json report and writes it to the given file. */
	public static <T> void writeReportToFile(File reportFile, T report) throws IOException {
		File directory = reportFile.getParentFile();
		if (!directory.isDirectory() && !directory.mkdirs()) {
			throw new IOException("Failed to create directory " + directory.getAbsolutePath());
		}
		try (BufferedSink writer = Okio.buffer(Okio.sink(reportFile))) {
			JsonAdapter<T> test = moshi.adapter((Class<T>) report.getClass()).indent("\t");
			test.toJson(writer, report);
		}
	}

	/** Converts to given report to a json string. */
	public static <T> String getReportAsString(T report) {
		return moshi.adapter((Class<T>) report.getClass()).indent("\t").toJson(report);
	}

	/** Recursively lists all files in the given directory that match the specified extension. */
	public static <T> List<T> readObjects(ETestArtifactFormat format, Class<T[]> clazz,
										  File... directoriesOrFiles) throws IOException {
		return readObjects(format, clazz, Arrays.asList(directoriesOrFiles));
	}

	/** Recursively lists all files in the given directory that match the specified extension. */
	public static <T> List<T> readObjects(ETestArtifactFormat format, Class<T[]> clazz,
										  List<File> directoriesOrFiles) throws IOException {
		List<File> files = listFiles(format, directoriesOrFiles);
		ArrayList<T> result = new ArrayList<>();
		for (File file : files) {
			try (BufferedSource source = Okio.buffer(Okio.source(file))) {
				T[] t = moshi.adapter(clazz).fromJson(source);
				if (t != null) {
					result.addAll(Arrays.asList(t));
				}
			}
		}
		return result;
	}

	/** Recursively lists all files of the given artifact type. */
	public static List<File> listFiles(ETestArtifactFormat format, File... directoriesOrFiles) {
		return listFiles(format, Arrays.asList(directoriesOrFiles));
	}

	/** Recursively lists all files of the given artifact type. */
	public static List<File> listFiles(ETestArtifactFormat format, List<File> directoriesOrFiles) {
		return directoriesOrFiles.stream().flatMap(directory -> FileSystemUtils.listFilesRecursively(directory,
				pathname -> pathname.isFile() && pathname.getName().startsWith(format.filePrefix) && FileSystemUtils
						.getFileExtension(pathname).equalsIgnoreCase(format.extension)).stream()).collect(toList());
	}
}
