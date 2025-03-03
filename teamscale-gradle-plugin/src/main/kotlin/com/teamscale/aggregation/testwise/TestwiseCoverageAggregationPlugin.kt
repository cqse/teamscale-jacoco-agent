package com.teamscale.aggregation.testwise

import com.teamscale.TeamscalePlugin
import com.teamscale.aggregation.ReportAggregationPlugin
import com.teamscale.aggregation.testwise.internal.DefaultAggregateTestwiseCoverageReport
import com.teamscale.utils.PartialData
import com.teamscale.utils.testwiseCoverageResults
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reporting.ReportingExtension
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject


/**
 * TODO
 * Root entry point for the Teamscale Gradle plugin.
 *
 * The plugin applies the Java plugin and a root extension named teamscale.
 * Each Test task configured in the project the plugin creates a new task suffixed with {@value #impactedTestsSuffix}
 * that executes the same set of tests, but additionally collects testwise coverage and executes only impacted tests.
 * Furthermore, all reports configured are uploaded to Teamscale after the tests have been executed.
 */
abstract class TestwiseCoverageAggregationPlugin : Plugin<Project> {

//	companion object {
//
//		const val TESTWISE_COVERAGE_AGGREGATION_CONFIGURATION_NAME = "testwiseCoverageAggregation"
//
//	}

//	@get:Inject
//	protected abstract val jvmPluginServices: JvmPluginServices

	@get:Inject
	protected abstract val objectFactory: ObjectFactory

	/** Applies the teamscale plugin against the given project.  */
	override fun apply(project: Project) {
		project.plugins.apply(TeamscalePlugin::class.java)

		val configurations = project.configurations
//		val testwiseCoverageAggregation =
//			configurations.dependencyScope(TESTWISE_COVERAGE_AGGREGATION_CONFIGURATION_NAME) {
//				description = "A configuration to collect testwise coverage reports across projects."
//				isVisible = false
//			}.get()

//		val testwiseCoverageResultsConfiguration = configurations.resolvable("aggregateTestwiseCoverageReportResults") {
//			extendsFrom(testwiseCoverageAggregation)
//			// TODO "Resolvable configuration used to gather files for the JaCoCo coverage report aggregation via ArtifactViews, not intended to be used directly"
//			description = "Graph needed for the aggregated test results report."
//			isVisible = false
//		}.get()

		val reporting = project.extensions.getByType(ReportingExtension::class.java)
		reporting.reports.registerBinding(
			AggregateTestwiseCoverageReport::class.java,
			DefaultAggregateTestwiseCoverageReport::class.java
		)

//		project.plugins.withType<JavaBasePlugin> {
//			testwiseCoverageAggregation.dependencies.add(project.dependencyFactory.create(project))
//			// If the current project is jvm-based, aggregate dependent projects as jvm-based as well.
//			jvmPluginServices.configureAsRuntimeClasspath(testwiseCoverageResultsConfiguration)
//		}

		val codeCoverageResultsConf = configurations.getByName(ReportAggregationPlugin.RESOLVABLE_REPORT_AGGREGATION_CONFIGURATION_NAME)

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
				}.artifacts.resolvedArtifacts.map { it.any { artifact -> artifact.variant.attributes.getAttribute(PartialData.PARTIAL_DATA_ATTRIBUTE) ?: false } })
			}
		}
	}
}
