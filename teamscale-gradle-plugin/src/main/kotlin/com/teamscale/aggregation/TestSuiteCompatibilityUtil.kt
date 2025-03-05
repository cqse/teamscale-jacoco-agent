package com.teamscale.aggregation

import com.teamscale.utils.*
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named

/**
 * Helper class that allows users of the plugin
 * to use "com.teamscale.aggregation" also with projects that do not use JVM test suites yet.
 */
@Suppress("unused")
object TestSuiteCompatibilityUtil {

	/**
	 * Exposes the results produced by the given test task as results produced by a test suite with the given name.
	 * A test suite with this name does not need to exist though.
	 * This is necessary to aggregate those reports across projects via the "com.teamscale.aggregation" plugin.
	 * It reuses the same mechanism introduced by the
	 * [JVM Test Suites Plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html)
	 * and
	 * [JaCoCo Report Aggregation Plugin](https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html)
	 * but provides a way to use this without using JVM test suites yet.
	 */
	@JvmStatic
	fun exposeTestForAggregation(test: Test, suiteName: String) {
		exposeTestForAggregation(test.project.tasks.named<Test>(test.name), suiteName)
	}

	/**
	 * Exposes the results produced by the given test task provider as results produced by a test suite with the given name.
	 * A test suite with this name does not need to exist though.
	 * This is necessary to aggregate those reports across projects via the "com.teamscale.aggregation" plugin.
	 * It reuses the same mechanism introduced by the
	 * [JVM Test Suites Plugin](https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html)
	 * and
	 * [JaCoCo Report Aggregation Plugin](https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html)
	 * but provides a way to use this without using JVM test suites yet.
	 */
	@JvmStatic
	fun exposeTestForAggregation(testProvider: TaskProvider<out Test>, suiteName: String) {
		if (testProvider.get().jacoco.isEnabled) {
			createCoverageDataVariant(testProvider.get().project, suiteName).configure {
				outgoing.artifact(testProvider.map { it.jacoco.destinationFile!! }) {
					type = ArtifactTypeDefinition.BINARY_DATA_TYPE
				}
			}
		}

		exposeTestReportArtifactsForAggregation(testProvider.get().project, testProvider, suiteName)
	}

	internal fun exposeTestReportArtifactsForAggregation(
		project: Project,
		testProvider: TaskProvider<out Test>,
		suiteName: String
	) {
		createTestResultsVariant(project, suiteName).configure {
			outgoing.artifact(testProvider.map { it.reports.junitXml.outputLocation }) {
				type = ArtifactTypeDefinition.DIRECTORY_TYPE
			}
		}

		createTestwiseCoverageResultsVariant(project, suiteName).configure {
			attributes.attributeProvider(
				PartialData.PARTIAL_DATA_ATTRIBUTE,
				testProvider.map { it.teamscale.partial.get() })
			outgoing.artifact(testProvider.map { it.teamscale.agent.destination }) {
				type = ArtifactTypeDefinition.DIRECTORY_TYPE
			}
		}
	}

	private fun createCoverageDataVariant(
		project: Project,
		suiteName: String
	): NamedDomainObjectProvider<ConsumableConfiguration> {
		val variantName = "binaryCoverageDataElementsFor${suiteName.capitalized()}"

		return project.configurations.consumable(variantName) {
			description = "Binary results containing Jacoco test coverage for '$suiteName' Tests."
			attributes.jacocoResults(project.objects, project.provider { suiteName })
		}
	}

	private fun createTestResultsVariant(
		project: Project,
		suiteName: String
	): NamedDomainObjectProvider<ConsumableConfiguration> {
		val variantName = "junitReportElementsFor${suiteName.capitalized()}"

		return project.configurations.consumable(variantName) {
			description = "JUnit results obtained from running the '$suiteName' Tests."
			attributes.junitReports(project.objects, project.provider { suiteName })
		}
	}

	private fun createTestwiseCoverageResultsVariant(
		project: Project,
		suiteName: String
	): NamedDomainObjectProvider<ConsumableConfiguration> {
		val variantName = "testwiseCoverageReportElementsFor${suiteName.capitalized()}"

		return project.configurations.consumable(variantName) {
			description = "Testwise coverage results obtained from running the '$suiteName' Tests."
			attributes.testwiseCoverageResults(project.objects, project.provider { suiteName })
		}
	}
}

fun String.capitalized() = this.replaceFirstChar { it.uppercase() }
