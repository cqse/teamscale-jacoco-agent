package com.teamscale.test_impacted.engine;

import com.teamscale.client.TestDetails;
import com.teamscale.report.ReportUtils;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.test_impacted.commons.LoggerUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Class for writing test data to a report directory. */
public class TestDataWriter {

	private static final Logger LOGGER = LoggerUtils.getLogger(TestDataWriter.class);

	private final File reportDirectory;

	public TestDataWriter(File reportDirectory) {
		this.reportDirectory = reportDirectory;
	}

	/** Writes the given test executions to a report file. */
	void dumpTestExecutions(List<TestExecution> testExecutions) {
		File file = new File(reportDirectory, "test-execution.json");
		try {
			ReportUtils.writeReportToFile(file, testExecutions);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e, () -> "Error while writing report to file: " + file);
		}
	}

	/** Writes the given test details to a report file. */
	void dumpTestDetails(List<? extends TestDetails> testDetails) {
		File file = new File(reportDirectory, "test-list.json");
		try {
			ReportUtils.writeReportToFile(file, new ArrayList<>(testDetails));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e, () -> "Error while writing report to file: " + file);
		}
	}
}
