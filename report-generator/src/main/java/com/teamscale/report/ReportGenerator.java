package com.teamscale.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/** Utilities for generating reports. */
public class ReportGenerator {

	private static final Gson GSON;

	static {
		GSON = new GsonBuilder().setPrettyPrinting().create();
	}

	/** Converts to given testwise coverage report to a json report and writes it to the given file. */
	public static <T> void writeReportToFile(File reportFile, T report) throws IOException {
		File directory = reportFile.getParentFile();
		if (!directory.isDirectory() && !directory.mkdirs()) {
			throw new IOException("Failed to create directory " + directory.getAbsolutePath());
		}
		try (FileWriter writer = new FileWriter(reportFile)) {
			GSON.toJson(report, writer);
		}
	}

	/** Converts to given report to a json string. */
	public static <T> String getReportAsString(T report) {
		return GSON.toJson(report);
	}
}
