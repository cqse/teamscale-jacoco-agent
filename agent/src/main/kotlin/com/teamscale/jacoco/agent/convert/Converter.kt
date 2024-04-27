package com.teamscale.jacoco.agent.convert

import com.teamscale.client.TestDetails
import com.teamscale.jacoco.agent.options.AgentOptionParseException
import com.teamscale.jacoco.agent.util.Benchmark.benchmark
import com.teamscale.jacoco.agent.util.Logging.logger
import com.teamscale.jacoco.agent.util.LoggingUtils.wrap
import com.teamscale.report.ReportUtils.filterByFormat
import com.teamscale.report.ReportUtils.readObjects
import com.teamscale.report.jacoco.EmptyReportException
import com.teamscale.report.jacoco.JaCoCoXmlReportGenerator
import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.testwise.ETestArtifactFormat
import com.teamscale.report.testwise.TestwiseCoverageReportWriter
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.factory.TestInfoFactory
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.CommandLineLogger
import org.jacoco.core.tools.ExecFileLoader
import java.io.IOException
import java.nio.file.Paths

/** Converts one .exec binary coverage file to XML. */
class Converter(private val arguments: ConvertCommand) {

	/** Converts one .exec binary coverage file to XML. */
	@Throws(IOException::class, AgentOptionParseException::class)
	fun runJaCoCoReportGeneration() {
		benchmark("Generating the XML report") {
			val loader = ExecFileLoader().apply {
				arguments.getInputFiles()
					.filterByFormat(ETestArtifactFormat.JACOCO)
					.forEach { load(it) }
			}
			val sessionInfo = loader.sessionInfoStore.getMerged("merged")
			val executionDataStore = loader.executionDataStore

			try {
				JaCoCoXmlReportGenerator(
					arguments.getClassDirectoriesOrZips(),
					wildcardIncludeExcludeFilter,
					arguments.duplicateClassFileBehavior,
					arguments.shouldIgnoreUncoveredClasses,
					wrap(logger)
				).convert(
					Dump(sessionInfo, executionDataStore),
					Paths.get(arguments.outputFile).toFile()
				)
			} catch (e: EmptyReportException) {
				logger.warn(e.localizedMessage)
			}
		}
	}

	/** Converts one .exec binary coverage file, test details and test execution files to JSON testwise coverage. */
	@Throws(IOException::class, AgentOptionParseException::class)
	fun runTestwiseCoverageReportGeneration() {
		val testDetails = arguments.getInputFiles().readObjects<TestDetails>(
			ETestArtifactFormat.TEST_LIST
		)
		val testExecutions = arguments.getInputFiles().readObjects<TestExecution>(
			ETestArtifactFormat.TEST_EXECUTION
		)
		val logger = CommandLineLogger()
		benchmark("Generating the testwise coverage report") {
			logger.info(
				"Writing report with " + testDetails.size + " Details/" + testExecutions.size + " Results"
			)

			TestwiseCoverageReportWriter(
				TestInfoFactory(testDetails, testExecutions),
				arguments.getOutputFile(),
				arguments.splitAfter
			).use { coverageWriter ->
				arguments.getInputFiles()
					.filterByFormat(ETestArtifactFormat.JACOCO)
					.forEach { executionDataFile ->
						JaCoCoTestwiseReportGenerator(
							arguments.getClassDirectoriesOrZips(),
							wildcardIncludeExcludeFilter,
							arguments.duplicateClassFileBehavior,
							logger
						).convertAndConsume(executionDataFile, coverageWriter)
					}
			}
		}
	}

	private val wildcardIncludeExcludeFilter: ClasspathWildcardIncludeFilter
		get() = ClasspathWildcardIncludeFilter(
			arguments.locationIncludeFilters.joinToString(":"),
			arguments.locationExcludeFilters.joinToString(":")
		)
}