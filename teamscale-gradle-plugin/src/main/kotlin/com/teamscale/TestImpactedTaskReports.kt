package com.teamscale

import org.gradle.api.reporting.SingleFileReport
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.testing.TestTaskReports

interface TestImpactedTaskReports : TestTaskReports {

	/**
	 * A JSON report representing the coverage on a per-test level.
	 *
	 * @return The JSON report
	 */
	@get:Internal
	val testwiseCoverage: SingleFileReport
}
