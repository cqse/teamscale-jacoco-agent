package com.teamscale.test_impacted.engine

import com.teamscale.client.TestDetails
import com.teamscale.report.ReportUtils.writeTestExecutionReport
import com.teamscale.report.ReportUtils.writeTestListReport
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.test_impacted.commons.LoggerUtils.createLogger
import java.io.File
import java.io.IOException
import java.util.logging.Level

/** Class for writing test data to a report directory.  */
open class TestDataWriter(private val reportDirectory: File) {
	/** Writes the given test executions to a report file.  */
	fun dumpTestExecutions(testExecutions: List<TestExecution>) {
		val file = File(reportDirectory, "test-execution.json")
		try {
			writeTestExecutionReport(file, testExecutions)
		} catch (e: IOException) {
			LOG.log(Level.SEVERE, e) { "Error while writing report to file: $file" }
		}
	}

	/** Writes the given test details to a report file.  */
	fun dumpTestDetails(testDetails: List<TestDetails>) {
		val file = File(reportDirectory, "test-list.json")
		try {
			writeTestListReport(file, ArrayList(testDetails))
		} catch (e: IOException) {
			LOG.log(Level.SEVERE, e) { "Error while writing report to file: $file" }
		}
	}

	companion object {
		private val LOG = createLogger()
	}
}
