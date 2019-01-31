package com.teamscale.report.testwise.jacoco

import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.util.ILogger
import com.teamscale.report.util.Predicate
import org.jacoco.core.data.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

/**
 * Creates a XML report for an execution data store. The report is grouped by session.
 *
 *
 * The class files under test must be compiled with debug information otherwise no coverage will be collected.
 */
class JaCoCoTestwiseReportGenerator
/**
 * Create a new generator with a collection of class directories.
 *
 * @param codeDirectoriesOrArchives Root directory that contains the projects class files.
 * @param locationIncludeFilter     Filter for class files
 * @param logger                    The logger
 */
@Throws(CoverageGenerationException::class)
constructor(
    codeDirectoriesOrArchives: Collection<File>,
    locationIncludeFilter: Predicate<String>,
    ignoreNonidenticalDuplicateClassFiles: Boolean,
    logger: ILogger
) {

    /** The execution data reader and converter.  */
    private val executionDataReader: CachingExecutionDataReader

    init {
        this.executionDataReader = CachingExecutionDataReader(logger)
        this.executionDataReader.analyzeClassDirs(
            codeDirectoriesOrArchives,
            locationIncludeFilter,
            ignoreNonidenticalDuplicateClassFiles
        )
    }

    /** Converts the given dumps to a report.  */
    @Throws(IOException::class)
    fun convert(executionDataFiles: Collection<File>): TestwiseCoverage {
        val aggregatedTestwiseCoverage = TestwiseCoverage()
        for (executionDataFile in executionDataFiles) {
            aggregatedTestwiseCoverage.add(executionDataReader.buildCoverage(readDumps(executionDataFile)))
        }
        return aggregatedTestwiseCoverage
    }

    /** Converts the given dumps to a report.  */
    @Throws(IOException::class)
    fun convert(executionDataFile: File): TestwiseCoverage {
        return executionDataReader.buildCoverage(readDumps(executionDataFile))
    }

    /** Reads the dumps from the given *.exec file.  */
    @Throws(IOException::class)
    private fun readDumps(executionDataFile: File): List<Dump> {
        val input = FileInputStream(executionDataFile)
        val executionDataReader = ExecutionDataReader(input)
        val dumpCollector = DumpCollector()
        executionDataReader.setExecutionDataVisitor(dumpCollector)
        executionDataReader.setSessionInfoVisitor(dumpCollector)
        executionDataReader.read()
        return dumpCollector.dumps
    }

    /** Collects dumps per session.  */
    private class DumpCollector : IExecutionDataVisitor, ISessionInfoVisitor {

        /** List of dumps.  */
        val dumps: MutableList<Dump> = ArrayList()

        /** The store to which coverage is currently written to.  */
        private var store: ExecutionDataStore? = null

        override fun visitSessionInfo(info: SessionInfo) {
            store = ExecutionDataStore()
            dumps.add(Dump(info, store!!))
        }

        override fun visitClassExecution(data: ExecutionData) {
            store!!.put(data)
        }
    }
}