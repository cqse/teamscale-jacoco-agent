package com.teamscale.aggregation.junit

import com.teamscale.aggregation.ReportAggregationPlugin
import com.teamscale.aggregation.aggregateReportResults
import com.teamscale.aggregation.filterProject
import com.teamscale.aggregation.junit.internal.DefaultAggregateJUnitReport
import com.teamscale.utils.artifactType
import com.teamscale.utils.junitReports
import com.teamscale.utils.reporting
import com.teamscale.utils.testing
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.withType
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

	/** Object factory. */
	@get:Inject
	protected abstract val objectFactory: ObjectFactory

	/** Applies the teamscale plugin against the given project.  */
	override fun apply(project: Project) {
		val reporting = project.reporting
		reporting.reports.registerBinding(
			AggregateJUnitReport::class.java,
			DefaultAggregateJUnitReport::class.java
		)

		val codeCoverageResultsConf = project.configurations.aggregateReportResults

		// Iterate and configure each user-specified report.
		reporting.reports.withType<AggregateJUnitReport> {
			reportTask.configure {
				include("**/*.xml")
				from(codeCoverageResultsConf.incoming.artifactView {
					withVariantReselection()
					filterProject()
					attributes.junitReports(objectFactory, testSuiteName)
					attributes.artifactType(ArtifactTypeDefinition.DIRECTORY_TYPE)
				}.files)
				into(project.layout.buildDirectory.dir("reports/jacoco/$name"))
			}
		}

		// convention for synthesizing reports based on existing test suites in "this" project
		project.plugins.withType<JvmTestSuitePlugin> {
			project.testing.suites.withType<JvmTestSuite> {
				val suite = this
				reporting.reports.create("${suite.name}AggregateJUnitReport", AggregateJUnitReport::class.java) {
					testSuiteName.convention(suite.name)
				}
			}
		}
	}
}
