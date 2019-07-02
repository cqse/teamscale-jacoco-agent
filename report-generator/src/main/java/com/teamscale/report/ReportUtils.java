package com.teamscale.report;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.TestDetails;
import com.teamscale.report.testwise.ETestArtifactFormat;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
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

	private static JsonAdapter<List<TestDetails>> testDetailsAdapter = moshi.<List<TestDetails>>adapter(
			Types.newParameterizedType(List.class, TestDetails.class)).indent("\t");

	private static JsonAdapter<List<TestExecution>> testExecutionAdapter = moshi.<List<TestExecution>>adapter(
			Types.newParameterizedType(List.class, TestExecution.class)).indent("\t");

	private static JsonAdapter<TestwiseCoverageReport> testwiseCoverageReportAdapter = moshi
			.adapter(TestwiseCoverageReport.class).indent("\t");

	/** Converts to given test list to a json report and writes it to the given file. */
	public static void writeTestListReport(File reportFile, List<TestDetails> report) throws IOException {
		writeReportToFile(reportFile, report, testDetailsAdapter);
	}

	/** Converts to given test execution report to a json report and writes it to the given file. */
	public static void writeTestExecutionReport(File reportFile, List<TestExecution> report) throws IOException {
		writeReportToFile(reportFile, report, testExecutionAdapter);
	}

	/** Converts to given testwise coverage report to a json report and writes it to the given file. */
	public static void writeTestwiseCoverageReport(File reportFile, TestwiseCoverageReport report) throws IOException {
		writeReportToFile(reportFile, report, testwiseCoverageReportAdapter);
	}

	/** Converts to given report to a json string. For testing only. */
	public static String getTestwiseCoverageReportAsString(TestwiseCoverageReport report) {
		return testwiseCoverageReportAdapter.toJson(report);
	}

	/** Writes the report object to the given file as json. */
	private static <T> void writeReportToFile(File reportFile, T report, JsonAdapter<T> adapter) throws IOException {
		File directory = reportFile.getParentFile();
		if (!directory.isDirectory() && !directory.mkdirs()) {
			throw new IOException("Failed to create directory " + directory.getAbsolutePath());
		}
		try (BufferedSink sink = Okio.buffer(Okio.sink(reportFile))) {
			adapter.toJson(sink, report);
		}
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
	public static List<File> listFiles(ETestArtifactFormat format, List<File> directoriesOrFiles) {
		return directoriesOrFiles.stream().flatMap(directory -> FileSystemUtils.listFilesRecursively(directory,
				pathname -> pathname.isFile() && pathname.getName().startsWith(format.filePrefix) && FileSystemUtils
						.getFileExtension(pathname).equalsIgnoreCase(format.extension)).stream()).collect(toList());
	}
}
