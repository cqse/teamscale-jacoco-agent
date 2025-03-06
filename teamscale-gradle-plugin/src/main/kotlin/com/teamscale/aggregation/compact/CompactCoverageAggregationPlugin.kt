package com.teamscale.aggregation.compact

import com.teamscale.aggregation.ReportAggregationPlugin
import com.teamscale.aggregation.compact.internal.DefaultAggregateCompactCoverageReport
import com.teamscale.utils.jacocoResults
import com.teamscale.utils.reporting
import com.teamscale.utils.testing
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject

/**
 * Plugin that supports aggregating binary JaCoCo coverage data across projects.
 *
 * It reuses the resolvable configuration (aggregateReportResults) from the [ReportAggregationPlugin]
 * to collect the classes and binary execution data files.
 *
 * @see [ReportAggregationPlugin] for details
 */
abstract class CompactCoverageAggregationPlugin : Plugin<Project> {

	@get:Inject
	protected abstract val objectFactory: ObjectFactory

	override fun apply(project: Project) {
		project.plugins.apply(ReportingBasePlugin::class.java)

		val reporting = project.reporting
		reporting.reports.registerBinding(
			AggregateCompactCoverageReport::class.java,
			DefaultAggregateCompactCoverageReport::class.java
		)

		val codeCoverageResultsConfiguration =
			project.configurations.getByName(ReportAggregationPlugin.RESOLVABLE_REPORT_AGGREGATION_CONFIGURATION_NAME)

		val classDirectories = codeCoverageResultsConfiguration.incoming.artifactView {
			componentFilter { it is ProjectComponentIdentifier }
			attributes {
				attribute(
					LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
					objectFactory.named<LibraryElements>(LibraryElements.CLASSES)
				)
			}
		}.files


		// Iterate and configure each user-specified report.
		reporting.reports.withType<AggregateCompactCoverageReport> {
			reportTask.configure {
				val executionData = codeCoverageResultsConfiguration.incoming.artifactView {
					withVariantReselection()
					componentFilter { it is ProjectComponentIdentifier }
					attributes {
						jacocoResults(objectFactory, testSuiteName)
						attribute(
							ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
							ArtifactTypeDefinition.BINARY_DATA_TYPE
						)
					}
				}.files
				this.executionData.from(executionData)
				dependsOn(executionData.buildDependencies)
				this.classDirectories.from(classDirectories)
				dependsOn(classDirectories.buildDependencies)
			}
		}

		// convention for synthesizing reports based on existing test suites in "this" project
		project.plugins.withType<JvmTestSuitePlugin> {
			project.testing.suites.withType<JvmTestSuite> {
				val suite = this
				reporting.reports.create(
					"${suite.name}AggregateCompactCoverageReport",
					AggregateCompactCoverageReport::class.java
				) {
					testSuiteName.convention(suite.name)
				}
			}
		}
	}
}
