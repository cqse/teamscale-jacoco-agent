package com.teamscale.aggregation.testwise

import com.teamscale.TeamscalePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
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
abstract class TestwiseCoverageBinaryAggregationPlugin : Plugin<Project> { //TODO rmeove

	companion object {

		const val TESTWISE_COVERAGE_AGGREGATION_CONFIGURATION_NAME = "testwiseCoverageAggregation"

	}

	@get:Inject
	protected abstract val jvmPluginServices: JvmPluginServices

//	@get:Inject
//	protected abstract val objectFactory: ObjectFactory

	/** Applies the teamscale plugin against the given project.  */
	override fun apply(project: Project) {
		project.plugins.apply(TeamscalePlugin::class.java)

		val configurations = project.configurations
		val testwiseCoverageAggregation =
			configurations.dependencyScope(TESTWISE_COVERAGE_AGGREGATION_CONFIGURATION_NAME) {
				description = "A configuration to collect testwise coverage reports across projects."
				isVisible = false
			}.get()

		val testwiseCoverageResultsConfiguration = configurations.resolvable("aggregateTestwiseCoverageReportResults") {
			extendsFrom(testwiseCoverageAggregation)
			// TODO "Resolvable configuration used to gather files for the JaCoCo coverage report aggregation via ArtifactViews, not intended to be used directly"
			description = "Graph needed for the aggregated test results report."
			isVisible = false
		}.get()



//		val reporting = project.extensions.getByType(ReportingExtension::class.java)
//		reporting.reports.registerBinding(
//			AggregateCompactCoverageReport::class.java,
//			DefaultAggregateCompactCoverageReport::class.java
//		)

//		val testReportDirectory: DirectoryProperty = objectFactory.directoryProperty().convention(
//			reporting.baseDirectory.dir("compact-coverage") //TODO how do other gradle plugins do the casing?
//		)

		project.plugins.withType<JavaBasePlugin> {
			testwiseCoverageAggregation.dependencies.add(project.dependencyFactory.create(project))
			// If the current project is jvm-based, aggregate dependent projects as jvm-based as well.
			jvmPluginServices.configureAsRuntimeClasspath(testwiseCoverageResultsConfiguration)
		}

//		val codeCoverageResultsConf = configurations.named("aggregateCodeCoverageReportResults").get()
//
//		val classDirectories: ArtifactView = codeCoverageResultsConf.incoming.artifactView {
//			componentFilter { it is ProjectComponentIdentifier }
//			attributes {
//				attribute(
//					LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
//					objectFactory.named<LibraryElements>(LibraryElements.CLASSES)
//				)
//			}
//		}


//		// Iterate and configure each user-specified report.
//		reporting.reports.withType<AggregateCompactCoverageReport> {
//			reportTask.configure {
//				this.classDirectories.from(classDirectories)
//				this.executionData.from(codeCoverageResultsConf.incoming.artifactView {
//					withVariantReselection()
//					componentFilter { it is ProjectComponentIdentifier }
//					attributes {
//						attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named<Category>(Category.VERIFICATION))
//						attributeProvider(
//							TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE,
//							testSuiteName.map { objectFactory.named<TestSuiteName>(it) })
//						attribute(
//							VerificationType.VERIFICATION_TYPE_ATTRIBUTE,
//							objectFactory.named<VerificationType>(VerificationType.JACOCO_RESULTS)
//						)
//						attribute(
//							ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
//							ArtifactTypeDefinition.BINARY_DATA_TYPE
//						)
//					}
//				})
//			}
//		}
	}
}
