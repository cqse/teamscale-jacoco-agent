package com.teamscale

import com.teamscale.client.ClusteredTestDetails
import com.teamscale.config.*
import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.ReportUtils
import com.teamscale.report.testwise.ETestArtifactFormat
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.ILogger
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.*
import java.io.File


/** Generates the actual testwise coverage report from the individual files produced by the Teamscale JaCoCo agent. */
class TestwiseCoverageReporting(
	private val logger: Logger,
	private val partial: Boolean,
	private val classDirs: MutableSet<File>,
	private val predicate: ClasspathWildcardIncludeFilter,
	private val reportOutputDir: File,
	private val reportOutputLocation: File
) {

	/** Generates a testwise coverage report and stores it on disk. */
	fun generateTestwiseCoverageReports() {
		val testDetails =
			ReportUtils.readObjects(
				ETestArtifactFormat.TEST_LIST,
				Array<ClusteredTestDetails>::class.java,
				listOf(reportOutputDir)
			)
		val testExecutions = ReportUtils.readObjects(
			ETestArtifactFormat.TEST_EXECUTION,
			Array<TestExecution>::class.java,
			listOf(reportOutputDir)
		)
		val jacocoExecutionData = ReportUtils.listFiles(ETestArtifactFormat.JACOCO, listOf(reportOutputDir))
		if (jacocoExecutionData.isEmpty()) {
			logger.error("No execution data provided!")
			return
		}

		val testwiseCoverage = buildTestwiseCoverage(jacocoExecutionData)

		logger.info("Generating report with ${testDetails.size} Details/${testwiseCoverage.tests.size} Coverage/${testExecutions.size} Results")

		val report = TestwiseCoverageReportBuilder.createFrom(
			testDetails,
			testwiseCoverage.tests.values,
			testExecutions,
			partial
		)
		logger.info("Writing report to ${reportOutputLocation.absolutePath}")
		ReportUtils.writeTestwiseCoverageReport(reportOutputLocation, report)
	}

	/** Collects JaCoCo's exec files from the artifacts folders and merges it with js coverage. */
	private fun buildTestwiseCoverage(jacocoExecutionData: List<File>): TestwiseCoverage {
		logger.info("Generating testwise coverage for $jacocoExecutionData")

		val jaCoCoTestwiseReportGenerator = JaCoCoTestwiseReportGenerator(
			classDirs,
			predicate,
			EDuplicateClassFileBehavior.IGNORE,
			logger.wrapInILogger()
		)

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
		override fun debug(message: String) = logger.debug(message)
		override fun info(message: String) = logger.info(message)
		override fun warn(message: String) = logger.warn(message)
		override fun warn(message: String, throwable: Throwable?) = logger.warn(message, throwable)
		override fun error(throwable: Throwable) = logger.error("", throwable)
		override fun error(message: String, throwable: Throwable?) = logger.error(message, throwable)
	}
}
