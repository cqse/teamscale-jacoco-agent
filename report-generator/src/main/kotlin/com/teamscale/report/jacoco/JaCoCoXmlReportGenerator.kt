package com.teamscale.report.jacoco

import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.ILogger
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IBundleCoverage
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfo
import org.jacoco.report.xml.XMLFormatter
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** Creates an XML report from binary execution data.  */
class JaCoCoXmlReportGenerator(
	/** Directories and zip files that contain class files.  */
	private val codeDirectoriesOrArchives: List<File>,
	/**
	 * Include filter to apply to all locations during class file traversal.
	 */
	private val locationIncludeFilter: ClasspathWildcardIncludeFilter,
	/** Whether to ignore non-identical duplicates of class files.  */
	private val duplicateClassFileBehavior: EDuplicateClassFileBehavior,
	/** Whether to remove uncovered classes from the report.  */
	private val ignoreUncoveredClasses: Boolean,
	/** The logger.  */
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
		val bundleCoverage = analyzeStructureAndAnnotateCoverage(mergedStore)
		checkForEmptyReport(bundleCoverage)
		coverageFile.outputStream.use { outputStream ->
			createReport(
				outputStream, bundleCoverage, dump.info,
				mergedStore!!
			)
		}
	}

	@Throws(EmptyReportException::class)
	private fun checkForEmptyReport(coverage: IBundleCoverage) {
		if (coverage.packages.size == 0 || coverage.lineCounter.totalCount == 0) {
			throw EmptyReportException("The generated coverage report is empty. " + MOST_LIKELY_CAUSE_MESSAGE)
		}
		if (coverage.lineCounter.coveredCount == 0) {
			throw EmptyReportException(
				"The generated coverage report does not contain any covered source code lines. " +
						MOST_LIKELY_CAUSE_MESSAGE
			)
		}
	}

	/**
	 * Analyzes the structure of the class files in [.codeDirectoriesOrArchives] and builds an in-memory coverage
	 * report with the coverage in the given store.
	 */
	@Throws(IOException::class)
	private fun analyzeStructureAndAnnotateCoverage(store: ExecutionDataStore?): IBundleCoverage {
		val coverageBuilder: CoverageBuilder = TeamscaleCoverageBuilder(
			this.logger,
			duplicateClassFileBehavior, ignoreUncoveredClasses
		)

		val analyzer = FilteringAnalyzer(
			store, coverageBuilder,
			locationIncludeFilter,
			logger
		)

		for (file in codeDirectoriesOrArchives) {
			analyzer.analyzeAll(file)
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
			output: OutputStream, bundleCoverage: IBundleCoverage, sessionInfo: SessionInfo?,
			store: ExecutionDataStore
		) {
			val xmlFormatter = XMLFormatter()
			val visitor = xmlFormatter.createVisitor(output)

			visitor.visitInfo(listOf(sessionInfo), store.contents)
			visitor.visitBundle(bundleCoverage, null)
			visitor.visitEnd()
		}
	}
}
