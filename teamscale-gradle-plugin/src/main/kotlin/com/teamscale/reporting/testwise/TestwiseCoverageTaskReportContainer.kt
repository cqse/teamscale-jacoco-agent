package com.teamscale.reporting.testwise

import org.gradle.api.reporting.Report
import org.gradle.api.reporting.ReportContainer
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.tasks.Internal

interface TestwiseCoverageTaskReportContainer : ReportContainer<Report> {

	/**
	 * A JSON report representing the coverage on a per-test level.
	 *
	 * @return The JSON report
	 */
	@get:Internal
	val testwiseCoverage: SingleFileReport
}
