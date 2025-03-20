package com.teamscale.aggregation.compact.internal

import com.teamscale.aggregation.compact.AggregateCompactCoverageReport
import com.teamscale.reporting.compact.CompactCoverageReport
import org.gradle.api.reporting.ReportSpec
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

/**
 * [ReportSpec] implementation that holds a [CompactCoverageReport] task for aggregating JaCoCo coverage to the
 * [Teamscale Compat Coverage](https://docs.teamscale.com/reference/upload-formats-and-samples/teamscale-compact-coverage/)
 * format across projects.
 */
internal abstract class DefaultAggregateCompactCoverageReport @Inject constructor(
	private val name: String,
	tasks: TaskContainer
) : AggregateCompactCoverageReport {

	override val reportTask = tasks.register<CompactCoverageReport>(name) {
		group = LifecycleBasePlugin.VERIFICATION_GROUP
		description = "Generates aggregated Teamscale Compact Coverage report."
	}

	override fun getName(): String {
		return name
	}
}
