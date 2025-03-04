package com.teamscale.aggregation.junit

import org.gradle.api.provider.Property
import org.gradle.api.reporting.ReportSpec
import org.gradle.api.tasks.TaskProvider

/**
 * [ReportSpec] that holds a [JUnitReportCollectionTask] task for aggregating JaCoCo coverage to the
 * [Teamscale Compat Coverage](https://docs.teamscale.com/reference/upload-formats-and-samples/teamscale-compact-coverage/)
 * format across projects.
 */
interface AggregateJUnitReport : ReportSpec {
	/**
	 * Contains the [JUnitReportCollectionTask] task instance which produces this report.
	 *
	 * @return the task instance
	 */
	val reportTask: TaskProvider<JUnitReportCollectionTask>

	/**
	 * Contains the name of the test suite in target projects that this report will aggregate.
	 *
	 * @return the name of the suite that this report will aggregate.
	 */
	val testSuiteName: Property<String>
}
