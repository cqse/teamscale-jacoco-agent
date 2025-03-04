package com.teamscale.aggregation.junit.internal

import com.teamscale.aggregation.junit.AggregateJUnitReport
import com.teamscale.aggregation.junit.JUnitReportCollectionTask
import org.gradle.api.reporting.ReportSpec
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

/**
 * [ReportSpec] implementation that holds a [JUnitReportCollectionTask] task for collecting JUnit reports across projects.
 */
internal abstract class DefaultAggregateJUnitReport @Inject constructor(
	private val name: String,
	tasks: TaskContainer
) : AggregateJUnitReport {

	override val reportTask = tasks.register<JUnitReportCollectionTask>(name) {
		group = LifecycleBasePlugin.VERIFICATION_GROUP
		description = "Collects JUnit reports from multiple other projects."
	}

	override fun getName(): String {
		return name
	}
}
