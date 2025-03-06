package com.teamscale.aggregation

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject


/**
 * Plugin that supports aggregating Testwise Coverage binary-data, JaCoCo binary data and JUnit reports across projects.
 *
 * It defines a consumable configuration (reportAggregation),
 * that is used to collect all projects to aggregate reports from and a resolvable configuration
 * (aggregateReportResults) that can be used by the upload task to resolve the reports.
 *
 * If the aggregating project is a Java project itself, we add the project as a dependency
 * to inherit all projects that are on the projects runtime classpath by default.
 *
 * If the aggregation project is standalone, it needs
 * to manually add a dependency on all projects that should be aggregated to the reportAggregation configuration.
 */
abstract class ReportAggregationPlugin : Plugin<Project> {

	companion object {

		private const val REPORT_AGGREGATION_CONFIGURATION_NAME = "reportAggregation"
		const val RESOLVABLE_REPORT_AGGREGATION_CONFIGURATION_NAME = "aggregateReportResults"

	}

	@get:Inject
	protected abstract val jvmPluginServices: JvmPluginServices

	override fun apply(project: Project) {
		val configurations = project.configurations
		val reportAggregation =
			configurations.dependencyScope(REPORT_AGGREGATION_CONFIGURATION_NAME) {
				description = "A configuration to collect reports across projects."
				isVisible = false
			}.get()

		val aggregateReportResults = configurations.resolvable(RESOLVABLE_REPORT_AGGREGATION_CONFIGURATION_NAME) {
			extendsFrom(reportAggregation)
			description =
				"Resolvable configuration used to gather files for report aggregation via ArtifactViews, not intended to be used directly"
			isVisible = false
		}.get()

		project.plugins.withType<JavaBasePlugin> {
			reportAggregation.dependencies.add(project.dependencyFactory.create(project))
			// If the current project is jvm-based, aggregate dependent projects as jvm-based as well.
			jvmPluginServices.configureAsRuntimeElements(aggregateReportResults)
		}
	}
}
