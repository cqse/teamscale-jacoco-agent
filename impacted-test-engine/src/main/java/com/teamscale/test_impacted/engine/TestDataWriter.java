package com.teamscale.test_impacted.engine;

import com.teamscale.client.TestDetails;
import com.teamscale.report.ReportUtils;
import com.teamscale.report.testwise.model.TestExecution;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Class for writing test data to a report directory. */
class TestDataWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDataWriter.class);

	private final File reportDirectory;

	TestDataWriter(File reportDirectory) {
		this.reportDirectory = reportDirectory;
	}

	void dumpTestExecutions(List<TestExecution> testExecutions) {
		writeReport(new File(reportDirectory, "test-execution.json"), testExecutions);
	}

	/** Writes the given test details to a report file. */
	void dumpTestDetails(List<TestDetails> testDetails) {
		writeReport(new File(reportDirectory, "test-list.json"), testDetails);
	}

	private static <T> void writeReport(File file, T report) {
		try {
			ReportUtils.writeReportToFile(file, report);
		} catch (IOException e) {
			LOGGER.error(e, () -> "Error while writing report to file: " + file);
		}
	}
}
