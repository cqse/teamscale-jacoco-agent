package com.teamscale.report.jacoco

import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.ILogger
import org.jacoco.core.analysis.IBundleCoverage
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfo
import org.jacoco.report.xml.XMLFormatter
import java.io.File
import java.io.IOException
import java.io.OutputStream

/**
 * Creates an XML report from binary execution data.
 *
 * @param codeDirectoriesOrArchives Directories and zip files that contain class files.
 * @param locationIncludeFilter Include filter to apply to all locations during class file traversal.
 * @param duplicateClassFileBehavior Whether to ignore non-identical duplicates of class files.
 * @param ignoreUncoveredClasses Whether to remove uncovered classes from the report.
 * @param logger The logger.
 */
class JaCoCoXmlReportGenerator(
	private val codeDirectoriesOrArchives: List<File>,
	private val locationIncludeFilter: ClasspathWildcardIncludeFilter,
	private val duplicateClassFileBehavior: EDuplicateClassFileBehavior,
	private val ignoreUncoveredClasses: Boolean,
	private val logger: ILogger
) {
	/**
	 * Creates the report and writes it to a file.
	 *
	 * @return The file object of for the converted report or null if it could not be created
	 */
	@Throws(IOException::class, EmptyReportException::class)
	fun convert(dump: Dump, filePath: File): CoverageFile {
		val coverageFile = CoverageFile(filePath)
		convertToReport(coverageFile, dump)
		return coverageFile
	}

	/** Creates the report.  */
	@Throws(IOException::class, EmptyReportException::class)
	private fun convertToReport(coverageFile: CoverageFile, dump: Dump) {
		val mergedStore = dump.store
		analyzeStructureAndAnnotateCoverage(mergedStore).apply {
			checkForEmptyReport()
			coverageFile.outputStream.use { outputStream ->
				createReport(
					outputStream, this, dump.info, mergedStore)
			}
		}
	}

	@Throws(EmptyReportException::class)
	private fun IBundleCoverage.checkForEmptyReport() {
		if (packages.isEmpty() || lineCounter.totalCount == 0) {
			throw EmptyReportException("The generated coverage report is empty. $MOST_LIKELY_CAUSE_MESSAGE")
		}
		if (lineCounter.coveredCount == 0) {
			throw EmptyReportException("The generated coverage report does not contain any covered source code lines. $MOST_LIKELY_CAUSE_MESSAGE")
		}
	}

	/**
	 * Analyzes the structure of the class files in [.codeDirectoriesOrArchives] and builds an in-memory coverage
	 * report with the coverage in the given store.
	 */
	@Throws(IOException::class)
	private fun analyzeStructureAndAnnotateCoverage(store: ExecutionDataStore): IBundleCoverage {
		val coverageBuilder = TeamscaleCoverageBuilder(
			logger, duplicateClassFileBehavior, ignoreUncoveredClasses
		)

		codeDirectoriesOrArchives.forEach { file ->
			FilteringAnalyzer(store, coverageBuilder, locationIncludeFilter, logger)
				.analyzeAll(file)
		}

		return coverageBuilder.getBundle("dummybundle")
	}

	companion object {
		/** Part of the error message logged when validating the coverage report fails.  */
		private const val MOST_LIKELY_CAUSE_MESSAGE = "Most likely you did not configure the agent correctly." +
				" Please check that the includes and excludes options are set correctly so the relevant code is included." +
				" If in doubt, first include more code and then iteratively narrow the patterns down to just the relevant code." +
				" If you have specified the class-dir option, please make sure it points to a directory containing the" +
				" class files/jars/wars/ears/etc. for which you are trying to measure code coverage."

		/** Creates an XML report based on the given session and coverage data.  */
		@Throws(IOException::class)
		private fun createReport(
			output: OutputStream,
			bundleCoverage: IBundleCoverage,
			sessionInfo: SessionInfo?,
			store: ExecutionDataStore
		) {
			XMLFormatter().createVisitor(output).apply {
				visitInfo(listOf(sessionInfo), store.contents)
				visitBundle(bundleCoverage, null)
				visitEnd()
			}
		}
	}
}
