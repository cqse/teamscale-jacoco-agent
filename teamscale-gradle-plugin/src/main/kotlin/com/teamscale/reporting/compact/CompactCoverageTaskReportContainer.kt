package com.teamscale.reporting.compact

import org.gradle.api.reporting.Report
import org.gradle.api.reporting.ReportContainer
import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.tasks.Internal

/**
 * The reports produced by the [CompactCoverageReport] task.
 */
interface CompactCoverageTaskReportContainer : ReportContainer<Report> {

	/**
	 * A JSON report representing the coverage in a compact form.
	 */
	@get:Internal
	val compactCoverage: SingleFileReport
}
