package com.teamscale.aggregation

import com.teamscale.aggregation.compact.CompactCoverageAggregationPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Wrapper plugin that applies our junit, testwise coverage, and compact coverage aggregation plugins.
 */
@Suppress("unused")
abstract class TeamscaleAggregationPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		project.logger.info("Applying coverage aggregation plugin to ${project.name}")
		project.plugins.apply(CompactCoverageAggregationPlugin::class.java)
		project.plugins.apply(ReportAggregationPlugin::class.java)
	}
}
