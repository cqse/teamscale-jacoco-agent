package com.teamscale.aggregation.testwise

import com.teamscale.aggregation.ReportAggregationPlugin
import com.teamscale.aggregation.aggregateReportResults
import com.teamscale.aggregation.classDirectories
import com.teamscale.aggregation.filterProject
import com.teamscale.aggregation.testwise.internal.DefaultAggregateTestwiseCoverageReport
import com.teamscale.utils.PartialData
import com.teamscale.utils.artifactType
import com.teamscale.utils.reporting
import com.teamscale.utils.testwiseCoverageResults
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject

/**
 * Plugin that supports aggregating binary data produced by the Teamscale JaCoCo agent across projects.
 *
 * It reuses the resolvable configuration (aggregateReportResults) from the [ReportAggregationPlugin]
 * to collect the directories that contain the binary exec files,
 * the test details and test execution results in JSON format.
 *
 * @see [ReportAggregationPlugin] for details
 */
abstract class TestwiseCoverageAggregationPlugin : Plugin<Project> {

	/** Object factory. */
	@get:Inject
	protected abstract val objectFactory: ObjectFactory

	/** Applies the teamscale plugin against the given project.  */
	override fun apply(project: Project) {
		val reporting = project.reporting
		reporting.reports.registerBinding(
			AggregateTestwiseCoverageReport::class.java,
			DefaultAggregateTestwiseCoverageReport::class.java
		)

		val codeCoverageResultsConf = project.configurations.aggregateReportResults
		val classDirectories = codeCoverageResultsConf.classDirectories(objectFactory)

		// Iterate and configure each user-specified report.
		reporting.reports.withType<AggregateTestwiseCoverageReport> {
			reportTask.configure {
				this.classDirectories.from(classDirectories)
				val executionDataView = codeCoverageResultsConf.incoming.artifactView {
					withVariantReselection()
					filterProject()
					attributes.testwiseCoverageResults(objectFactory, testSuiteName)
					attributes.artifactType(ArtifactTypeDefinition.DIRECTORY_TYPE)
				}
				this.executionData.from(executionDataView.files)
				this.partial.set(executionDataView.artifacts.resolvedArtifacts.map {
					it.any(ResolvedArtifactResult::isPartial)
				})
			}
		}
	}
}

private fun ResolvedArtifactResult.isPartial() =
	variant.attributes.getAttribute(PartialData.PARTIAL_DATA_ATTRIBUTE) ?: false
