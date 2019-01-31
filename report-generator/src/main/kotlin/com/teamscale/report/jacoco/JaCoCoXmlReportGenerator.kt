package com.teamscale.report.jacoco

import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.util.ILogger
import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IBundleCoverage
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfo
import org.jacoco.report.IReportVisitor
import org.jacoco.report.xml.XMLFormatter

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.Collections
import java.util.function.Predicate

/** Creates an XML report from binary execution data.  */
class JaCoCoXmlReportGenerator
/** Constructor.  */
    (
    /** Directories and zip files that contain class files.  */
    private val codeDirectoriesOrArchives: List<File>,
    /**
     * Include filter to apply to all locations during class file traversal.
     */
    private val locationIncludeFilter: Predicate<String>,
    /** Whether to ignore non-identical duplicates of class files.  */
    private val ignoreNonidenticalDuplicateClassFiles: Boolean,
    /** The logger.  */
    private val logger: ILogger
) {

    /** Creates the report.  */
    @Throws(IOException::class)
    fun convert(dump: Dump): String {
        val output = ByteArrayOutputStream()
        convertToReport(output, dump)
        return output.toString(FileSystemUtils.UTF8_ENCODING)
    }

    /** Creates the report.  */
    @Throws(IOException::class)
    fun convertToReport(output: OutputStream, dump: Dump) {
        val mergedStore = dump.store
        val bundleCoverage = analyzeStructureAndAnnotateCoverage(mergedStore)
        createReport(output, bundleCoverage, dump.info, mergedStore)
    }

    /** Creates an XML report based on the given session and coverage data.  */
    @Throws(IOException::class)
    private fun createReport(
        output: OutputStream,
        bundleCoverage: IBundleCoverage,
        sessionInfo: SessionInfo,
        store: ExecutionDataStore
    ) {
        val xmlFormatter = XMLFormatter()
        val visitor = xmlFormatter.createVisitor(output)

        visitor.visitInfo(listOf(sessionInfo), store.contents)
        visitor.visitBundle(bundleCoverage, null)
        visitor.visitEnd()
    }

    /**
     * Analyzes the structure of the class files in
     * [.codeDirectoriesOrArchives] and builds an in-memory coverage report
     * with the coverage in the given store.
     */
    @Throws(IOException::class)
    private fun analyzeStructureAndAnnotateCoverage(store: ExecutionDataStore): IBundleCoverage {
        var coverageBuilder = CoverageBuilder()
        if (ignoreNonidenticalDuplicateClassFiles) {
            coverageBuilder = DuplicateIgnoringCoverageBuilder(this.logger)
        }

        val analyzer = FilteringAnalyzer(store, coverageBuilder, locationIncludeFilter, logger)

        for (file in codeDirectoriesOrArchives) {
            analyzer.analyzeAll(file)
        }

        return coverageBuilder.getBundle("dummybundle")
    }

}
