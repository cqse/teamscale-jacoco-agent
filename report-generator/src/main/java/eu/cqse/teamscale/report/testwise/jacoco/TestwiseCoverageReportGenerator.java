package eu.cqse.teamscale.report.testwise.jacoco;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverageReport;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

/** Utilities for testwise coverage reports. */
public class TestwiseCoverageReportGenerator {

	private static final Gson GSON;

	static {
		GSON = new GsonBuilder().setPrettyPrinting().create();
	}

	/** Converts to given testwise coverage report to a json report and writes it to the given file. */
	public static void writeReportToFile(File report, TestwiseCoverageReport testwiseCoverage) throws IOException {
		File directory = report.getParentFile();
		if (!directory.isDirectory() && !directory.mkdirs()) {
			throw new IOException("Failed to create directory " + directory.getAbsolutePath());
		}
		try (FileWriter writer = new FileWriter(report)) {
			GSON.toJson(testwiseCoverage, writer);
		}
	}

	/** Converts to given testwise coverage to a json report. */
	public static String getReportAsString(TestwiseCoverageReport testwiseCoverage) {
		return GSON.toJson(testwiseCoverage);
	}
}
