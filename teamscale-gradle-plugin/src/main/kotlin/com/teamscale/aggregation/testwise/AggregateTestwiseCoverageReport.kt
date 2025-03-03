package com.teamscale.aggregation.testwise

import com.teamscale.reporting.compact.CompactCoverageReport
import com.teamscale.reporting.testwise.TestwiseCoverageReport
import org.gradle.api.provider.Property
import org.gradle.api.reporting.ReportSpec
import org.gradle.api.tasks.TaskProvider

/**
 * [ReportSpec] that holds a [TestwiseCoverageReport] task for aggregating JaCoCo coverage to the
 * [Testwise Coverage](https://docs.teamscale.com/reference/upload-formats-and-samples/testwise-coverage/)
 * format across projects.
 */
interface AggregateTestwiseCoverageReport : ReportSpec {
	/**
	 * Contains the [CompactCoverageReport] task instance which produces this report.
	 *
	 * @return the task instance
	 */
	val reportTask: TaskProvider<TestwiseCoverageReport>

	/**
	 * Contains the name of the test suite in target projects that this report will aggregate.
	 *
	 * @return the name of the suite that this report will aggregate.
	 */
	val testSuiteName: Property<String>
}
