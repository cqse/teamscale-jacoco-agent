package com.teamscale.reporting.compact

import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.compact.CompactCoverageReportGenerator
import com.teamscale.report.jacoco.EmptyReportException
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.reporting.JaCoCoBasedReportTaskBase
import com.teamscale.reporting.compact.internal.DefaultCompactCoverageTaskReportContainer
import com.teamscale.utils.reporting
import com.teamscale.utils.wrapInILogger
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.findByType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

/**
 * Task which generates a
 * [Teamscale Compact Coverage](https://docs.teamscale.com/reference/upload-formats-and-samples/teamscale-compact-coverage/)
 * report from binary JaCoCo coverage data.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class CompactCoverageReport : JaCoCoBasedReportTaskBase<CompactCoverageTaskReportContainer>() {

	override val reportContainer: CompactCoverageTaskReportContainer =
		objectFactory.newInstance(DefaultCompactCoverageTaskReportContainer::class.java)

	init {
		description = "Executes the impacted tests and collects coverage per test case"

		reportContainer.compactCoverage.required.convention(true)
		reportContainer.compactCoverage.outputLocation.convention(project.reporting.baseDirectory.file("compact-coverage/${name}/compact-coverage.json"))
	}

	/** Generates the Compact Coverage report */
	@TaskAction
	override fun generateReport() {
		if (!reportContainer.compactCoverage.required.get()) {
			return
		}
		logger.info("Generating compact coverage report...")
		logger.debug("Class files: {}", classDirectories.files)
		logger.debug("Execution data files: {}", executionData.files)
		val generator = CompactCoverageReportGenerator(
			classDirectories.files.filter { it.exists() },
			ClasspathWildcardIncludeFilter(null, null),
			EDuplicateClassFileBehavior.IGNORE,
			logger.wrapInILogger()
		)

		try {
			generator.convertExecFilesToReport(
				executionData.files.filter { it.exists() },
				reportContainer.compactCoverage.outputLocation.get().asFile
			)
		} catch (e: EmptyReportException) {
			logger.warn("Converted report was empty.")
		}
	}

	/**
	 * Adds execution data generated by a task to the list of those used during coverage analysis. Only tasks with a [JacocoTaskExtension] will be included; all others will be ignored.
	 *
	 * @param tasks one or more tasks to add
	 */
	fun executionData(vararg tasks: Task) {
		for (task in tasks) {
			val extension = task.extensions.findByType<JacocoTaskExtension>()
			if (extension != null) {
				executionData(task.project.provider { extension.destinationFile })
				mustRunAfter(task)
			}
		}
	}
}


