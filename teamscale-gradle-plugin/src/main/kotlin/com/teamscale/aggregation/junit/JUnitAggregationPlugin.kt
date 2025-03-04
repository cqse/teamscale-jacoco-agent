package com.teamscale.aggregation.junit

import com.teamscale.aggregation.ReportAggregationPlugin
import com.teamscale.aggregation.junit.internal.DefaultAggregateJUnitReport
import com.teamscale.utils.junitReports
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.reporting.ReportingExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.base.TestingExtension
import javax.inject.Inject

/**
 * Plugin that supports collecting JUnit reports across projects.
 *
 * It reuses the resolvable configuration (aggregateReportResults) from the [ReportAggregationPlugin]
 * to collect the directories that contain the JUnit reports in XML format.
 *
 * @see [ReportAggregationPlugin] for details
 */
abstract class JUnitAggregationPlugin : Plugin<Project> {

	@get:Inject
	protected abstract val objectFactory: ObjectFactory

	/** Applies the teamscale plugin against the given project.  */
	override fun apply(project: Project) {
		val reporting = project.extensions.getByType(ReportingExtension::class.java)
		reporting.reports.registerBinding(
			AggregateJUnitReport::class.java,
			DefaultAggregateJUnitReport::class.java
		)

		val codeCoverageResultsConf =
			project.configurations.getByName(ReportAggregationPlugin.RESOLVABLE_REPORT_AGGREGATION_CONFIGURATION_NAME)

		// Iterate and configure each user-specified report.
		reporting.reports.withType<AggregateJUnitReport> {
			reportTask.configure {
				include("**/*.xml")
				from(codeCoverageResultsConf.incoming.artifactView {
					withVariantReselection()
					componentFilter { it is ProjectComponentIdentifier }
					attributes {
						junitReports(objectFactory, testSuiteName)
						attribute(
							ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
							ArtifactTypeDefinition.DIRECTORY_TYPE
						)
					}
				}.files)
				into(project.layout.buildDirectory.dir("reports/jacoco/$name"))
			}
		}

		// convention for synthesizing reports based on existing test suites in "this" project
		project.plugins.withType<JvmTestSuitePlugin> {
			val testing = project.extensions.getByType<TestingExtension>()
			testing.suites.withType<JvmTestSuite> {
				val suite = this
				reporting.reports.create("${suite.name}AggregateJUnitReport", AggregateJUnitReport::class.java) {
					testSuiteName.convention(suite.name)
				}
			}
		}
	}
}
