package com.teamscale.aggregation.compact

import com.teamscale.reporting.compact.CompactCoverageReport
import org.gradle.api.provider.Property
import org.gradle.api.reporting.ReportSpec
import org.gradle.api.tasks.TaskProvider

/**
 * [ReportSpec] that holds a [CompactCoverageReport] task for aggregating JaCoCo coverage to the
 * [Teamscale Compat Coverage](https://docs.teamscale.com/reference/upload-formats-and-samples/teamscale-compact-coverage/)
 * format across projects.
 */
interface AggregateCompactCoverageReport : ReportSpec {
	/**
	 * Contains the [CompactCoverageReport] task instance which produces this report.
	 *
	 * @return the task instance
	 */
	val reportTask: TaskProvider<CompactCoverageReport>

	/**
	 * Contains the name of the test suite in target projects that this report will aggregate.
	 *
	 * @return the name of the suite that this report will aggregate.
	 */
	val testSuiteName: Property<String>
}
