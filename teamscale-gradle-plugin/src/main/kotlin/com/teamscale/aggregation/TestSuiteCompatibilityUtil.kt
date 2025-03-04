package com.teamscale.aggregation

import com.teamscale.TestImpacted
import com.teamscale.config.extension.TeamscaleTestImpactedTaskExtension
import com.teamscale.utils.PartialData
import com.teamscale.utils.jacocoResults
import com.teamscale.utils.junitReports
import com.teamscale.utils.testwiseCoverageResults
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

/**
 * Helper class that allows users of the plugin
 * to use "com.teamscale.aggregation" also with projects that do not use JVM test suites yet.
 * Also, TestImpacted task outputs cannot be shared natively via JVM test suites yet.
 */
@Suppress("unused")
object TestSuiteCompatibilityUtil {

	@JvmStatic
	fun exposeTestForAggregation(testProvider: TaskProvider<out Test>, suiteName: String) {
		exposeTestForAggregation(testProvider.get(), suiteName)
	}

	@JvmStatic
	fun exposeTestForAggregation(test: Test, suiteName: String) {

		if (test.extensions.findByType<JacocoTaskExtension>()?.isEnabled == true) {
			createCoverageDataVariant(test.project, suiteName).configure {
				outgoing.artifact(
					// We need to use "map" here to carry over the producer information in the provider
					// https://github.com/gradle/gradle/issues/13242
					test.project.tasks.named<Test>(test.name)
						.map { it.extensions.getByType<JacocoTaskExtension>().destinationFile!! }) {
					builtBy(test)
					type = ArtifactTypeDefinition.BINARY_DATA_TYPE
				}
			}
		}

		exposeJUnitReportsForAggregation(test, suiteName)

		if (test is TestImpacted) {
			createTestwiseCoverageResultsVariant(test.project, suiteName).configure {
				attributes.attributeProvider(PartialData.PARTIAL_DATA_ATTRIBUTE, test.partial)
				outgoing.artifact(
					// We need to use "map" here to carry over the producer information in the provider
					// https://github.com/gradle/gradle/issues/13242
					test.project.tasks.named<TestImpacted>(test.name)
						.map { it.extensions.getByType<TeamscaleTestImpactedTaskExtension>().agent.destination }) {
					builtBy(test)
					type = ArtifactTypeDefinition.DIRECTORY_TYPE
				}
			}
		}
	}

	internal fun exposeJUnitReportsForAggregation(test: Test, suiteName: String) {
		createTestResultsVariant(test.project, suiteName).configure {
			outgoing.artifact(
				// We need to use "map" here to carry over the producer information in the provider
				// https://github.com/gradle/gradle/issues/13242
				test.project.tasks.named<Test>(test.name)
					.map { it.reports.junitXml.outputLocation }) {
				builtBy(test)
				type = ArtifactTypeDefinition.DIRECTORY_TYPE
			}
		}
	}

	private fun createCoverageDataVariant(
		project: Project,
		suiteName: String
	): NamedDomainObjectProvider<ConsumableConfiguration> {
		val variantName = "binaryCoverageDataElementsFor${suiteName.capitalizedCompat()}"

		return project.configurations.consumable(variantName) {
			description = "Binary results containing Jacoco test coverage for '$suiteName' Tests."
			attributes.jacocoResults(project.objects, project.provider { suiteName })
		}
	}

	private fun createTestResultsVariant(
		project: Project,
		suiteName: String
	): NamedDomainObjectProvider<ConsumableConfiguration> {
		val variantName = "junitReportElementsFor${suiteName.capitalizedCompat()}"

		return project.configurations.consumable(variantName) {
			description = "JUnit results obtained from running the '$suiteName' Tests."
			attributes.junitReports(project.objects, project.provider { suiteName })
		}
	}

	private fun createTestwiseCoverageResultsVariant(
		project: Project,
		suiteName: String
	): NamedDomainObjectProvider<ConsumableConfiguration> {
		val variantName = "testwiseCoverageReportElementsFor${suiteName.capitalizedCompat()}"

		return project.configurations.consumable(variantName) {
			description = "Testwise coverage results obtained from running the '$suiteName' Tests."
			attributes.testwiseCoverageResults(project.objects, project.provider { suiteName })
		}
	}
}

fun String.capitalizedCompat() = this.replaceFirstChar { it.uppercase() }
