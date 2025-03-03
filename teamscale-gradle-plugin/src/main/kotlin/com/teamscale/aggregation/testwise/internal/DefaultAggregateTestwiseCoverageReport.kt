package com.teamscale.aggregation.testwise.internal

import com.teamscale.aggregation.testwise.AggregateTestwiseCoverageReport
import com.teamscale.reporting.testwise.TestwiseCoverageReport
import org.gradle.api.reporting.ReportSpec
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

/**
 * [ReportSpec] that holds a [TestwiseCoverageReport] task for aggregating JaCoCo coverage to the
 * [Testwise Coverage](https://docs.teamscale.com/reference/upload-formats-and-samples/testwise-coverage/)
 * format across projects.
 */
internal abstract class DefaultAggregateTestwiseCoverageReport @Inject constructor(
	private val name: String,
	tasks: TaskContainer
) : AggregateTestwiseCoverageReport {

	override val reportTask = tasks.register<TestwiseCoverageReport>(name) {
		group = LifecycleBasePlugin.VERIFICATION_GROUP
		description = "Generates aggregated Testwise Coverage report."
	}

	override fun getName(): String {
		return name
	}
}
