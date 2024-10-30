package com.teamscale

import com.teamscale.client.ClusteredTestDetails
import com.teamscale.config.*
import com.teamscale.config.extension.TeamscaleTestImpactedTaskExtension
import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.ReportUtils
import com.teamscale.report.testwise.ETestArtifactFormat
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder
import com.teamscale.report.util.ILogger
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.*
import java.io.File

/** Task which runs the impacted tests. */
@Suppress("MemberVisibilityCanBePrivate")
open class TestwiseCoverageReportTask : DefaultTask() {

	/**
	 * Test task's name for which reports are generated.
	 */
	@Internal
	lateinit var testTaskName: String

	/**
	 * Reference to the configuration that should be used for this task.
	 */
	@Internal
	lateinit var configuration: TeamscaleTestImpactedTaskExtension

	/** Includes and excludes from jacoco which will control which classes will be profiled. */
	val agentFilter: SerializableFilter
		@Input
		get() = configuration.agent.getFilter()

	/** Mapping from report files to artifacts that should be included in the report. */
	@Internal
	val reportsToArtifacts = mutableMapOf<Report, MutableList<File>>()

	/** All directories that contain the relevant class files. */
	@InputFiles
	val classDirs = mutableSetOf<FileCollection>()

	/** The upload task that will be executed afterwards. */
	@Internal
	lateinit var uploadTask: TeamscaleUploadTask

	/** A flattened list of report files to process. Only needed for Gradle's up-to-date check. */
	@get:InputFiles
	val testArtifacts
		get() = reportsToArtifacts.values.flatten()

	/** The report files that will be produced by the task. */
	val reportFiles
		@OutputFiles
		get() = reportsToArtifacts.keys.map { it.reportFiles }

	init {
		group = "Teamscale"
		description = "Generates a testwise coverage report"
	}

	/** Adds a test artifact to the given report. */
	fun addTestArtifactsDirs(report: Report, testArtifactDestination: File) {
		val list = reportsToArtifacts.computeIfAbsent(report) { mutableListOf() }
		list.add(testArtifactDestination)
	}

	/**
	 * Generates a testwise coverage from the execution data and merges it with eventually existing closure coverage.
	 */
	@TaskAction
	fun generateTestwiseCoverageReports() {
		if (reportsToArtifacts.isEmpty()) {
			logger.info("Skipping coverage report generation (No reports configured)")
			return
		}
		val jaCoCoTestwiseReportGenerator = JaCoCoTestwiseReportGenerator(
			classDirs.flatMap { it.files },
			agentFilter.getPredicate(),
			EDuplicateClassFileBehavior.IGNORE,
			project.logger.wrapInILogger()
		)

		logger.info("Generating coverage reports...")
		for ((reportConfig, artifacts) in reportsToArtifacts.entries) {
			generateTestwiseCoverageReport(reportConfig, artifacts, jaCoCoTestwiseReportGenerator)
		}
	}

	/** Generates a testwise coverage report and stores it on disk. */
	private fun generateTestwiseCoverageReport(
		reportConfig: Report,
		artifacts: MutableList<File>,
		jaCoCoTestwiseReportGenerator: JaCoCoTestwiseReportGenerator
	) {
		val testDetails =
			ReportUtils.readObjects(ETestArtifactFormat.TEST_LIST, Array<ClusteredTestDetails>::class.java, artifacts)
		val testExecutions = ReportUtils.readObjects(
			ETestArtifactFormat.TEST_EXECUTION,
			Array<TestExecution>::class.java,
			artifacts
		)

		val testwiseCoverage = buildTestwiseCoverage(artifacts, jaCoCoTestwiseReportGenerator) ?: return

		logger.info("Merging report with ${testDetails.size} Details/${testwiseCoverage.tests.size} Coverage/${testExecutions.size} Results")

		val report = TestwiseCoverageReportBuilder.createFrom(
			testDetails,
			testwiseCoverage.tests.values,
			testExecutions,
			reportConfig.partial
		)
		logger.info("Writing report to ${reportConfig.reportFiles.singleFile.absolutePath}")
		ReportUtils.writeTestwiseCoverageReport(reportConfig.reportFiles.singleFile, report)
	}

	/** Collects JaCoCo's exec files from the artifacts folders and merges it with js coverage. */
	private fun buildTestwiseCoverage(
		artifacts: MutableList<File>,
		jaCoCoTestwiseReportGenerator: JaCoCoTestwiseReportGenerator
	): TestwiseCoverage? {
		val jacocoExecutionData = ReportUtils.listFiles(ETestArtifactFormat.JACOCO, artifacts)
		if (jacocoExecutionData.isEmpty()) {
			logger.error("No execution data provided!")
			return null
		}
		logger.info("Generating testwise coverage for $jacocoExecutionData")

		val testwiseCoverage = TestwiseCoverage()
		for (file in jacocoExecutionData) {
			testwiseCoverage.add(jaCoCoTestwiseReportGenerator.convert(file))
		}
		return testwiseCoverage
	}
}

/** Wraps the gradle log4j logger into an ILogger. */
fun Logger.wrapInILogger(): ILogger {
	val logger = this
	return object : ILogger {
		override fun debug(message: String?) = logger.debug(message)
		override fun info(message: String?) = logger.info(message)
		override fun warn(message: String?) = logger.warn(message)
		override fun warn(message: String?, throwable: Throwable?) = logger.warn(message, throwable)
		override fun error(throwable: Throwable) = logger.error("", throwable)
		override fun error(message: String?, throwable: Throwable?) = logger.error(message, throwable)
	}
}
