package com.teamscale.aggregation.testwise

import com.teamscale.TeamscalePlugin
import com.teamscale.aggregation.ReportAggregationPlugin
import com.teamscale.aggregation.testwise.internal.DefaultAggregateTestwiseCoverageReport
import com.teamscale.utils.PartialData
import com.teamscale.utils.reporting
import com.teamscale.utils.testwiseCoverageResults
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
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

	@get:Inject
	protected abstract val objectFactory: ObjectFactory

	/** Applies the teamscale plugin against the given project.  */
	override fun apply(project: Project) {
		project.plugins.apply(TeamscalePlugin::class.java)

		val reporting = project.reporting
		reporting.reports.registerBinding(
			AggregateTestwiseCoverageReport::class.java,
			DefaultAggregateTestwiseCoverageReport::class.java
		)

		val codeCoverageResultsConf =
			project.configurations.getByName(ReportAggregationPlugin.RESOLVABLE_REPORT_AGGREGATION_CONFIGURATION_NAME)

		val classDirectories = codeCoverageResultsConf.incoming.artifactView {
			componentFilter { it is ProjectComponentIdentifier }
			attributes {
				attribute(
					LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
					objectFactory.named<LibraryElements>(LibraryElements.CLASSES)
				)
			}
		}.files


		// Iterate and configure each user-specified report.
		reporting.reports.withType<AggregateTestwiseCoverageReport> {
			reportTask.configure {
				this.classDirectories.from(classDirectories)
				this.executionData.from(codeCoverageResultsConf.incoming.artifactView {
					withVariantReselection()
					componentFilter { it is ProjectComponentIdentifier }
					attributes {
						testwiseCoverageResults(objectFactory, testSuiteName)
						attribute(
							ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
							ArtifactTypeDefinition.DIRECTORY_TYPE
						)
					}
				}.files)
				this.partial.set(codeCoverageResultsConf.incoming.artifactView {
					withVariantReselection()
					componentFilter { it is ProjectComponentIdentifier }
					attributes {
						testwiseCoverageResults(objectFactory, testSuiteName)
						attribute(
							ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
							ArtifactTypeDefinition.DIRECTORY_TYPE
						)
					}
				}.artifacts.resolvedArtifacts.map {
					it.any { artifact ->
						artifact.variant.attributes.getAttribute(
							PartialData.PARTIAL_DATA_ATTRIBUTE
						) ?: false
					}
				})
			}
		}
	}
}
