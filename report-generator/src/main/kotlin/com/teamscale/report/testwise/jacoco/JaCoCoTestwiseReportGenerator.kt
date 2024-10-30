package com.teamscale.report.testwise.jacoco

import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.ILogger
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.ExecutionDataReader
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.IExecutionDataVisitor
import org.jacoco.core.data.ISessionInfoVisitor
import org.jacoco.core.data.SessionInfo
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.function.Consumer

/**
 * Creates an XML report for an execution data store. The report is grouped by session.
 *
 * The class files under test must be compiled with debug information otherwise no coverage will be collected.
 */
open class JaCoCoTestwiseReportGenerator(
	codeDirectoriesOrArchives: Collection<File>,
	private val locationIncludeFilter: ClasspathWildcardIncludeFilter,
	duplicateClassFileBehavior: EDuplicateClassFileBehavior,
	logger: ILogger
) {

	/** The execution data reader and converter. */
	private val executionDataReader: CachingExecutionDataReader = CachingExecutionDataReader(
		logger, codeDirectoriesOrArchives, locationIncludeFilter, duplicateClassFileBehavior
	)

	init {
		updateClassDirCache()
	}

	/** Updates the probe cache of the [ExecutionDataReader]. */
	open fun updateClassDirCache() {
		executionDataReader.analyzeClassDirs()
	}

	/** Converts the given dumps to a report. */
	@Throws(IOException::class, CoverageGenerationException::class)
	open fun convert(executionDataFile: File): TestwiseCoverage {
		val testwiseCoverage = TestwiseCoverage()
		val dumpConsumer = executionDataReader.buildCoverageConsumer(locationIncludeFilter, testwiseCoverage::add)
		readAndConsumeDumps(executionDataFile, dumpConsumer)
		return testwiseCoverage
	}

	/** Converts the given dump to a report. */
	@Throws(CoverageGenerationException::class)
	open fun convert(dump: Dump): TestCoverageBuilder? {
		val list = mutableListOf<TestCoverageBuilder>()
		val dumpConsumer = executionDataReader.buildCoverageConsumer(locationIncludeFilter, list::add)
		dumpConsumer.accept(dump)
		return if (list.size == 1) list[0] else null
	}

	/** Converts the given dumps to a report. */
	@Throws(IOException::class)
	open fun convertAndConsume(executionDataFile: File, consumer: Consumer<TestCoverageBuilder>) {
		val dumpConsumer = executionDataReader.buildCoverageConsumer(locationIncludeFilter, consumer)
		readAndConsumeDumps(executionDataFile, dumpConsumer)
	}

	/** Reads the dumps from the given *.exec file. */
	@Throws(IOException::class)
	private fun readAndConsumeDumps(executionDataFile: File, dumpConsumer: Consumer<Dump>) {
		BufferedInputStream(FileInputStream(executionDataFile)).use { input ->
			val executionDataReader = ExecutionDataReader(input)
			val dumpCallback = DumpCallback(dumpConsumer)
			executionDataReader.setExecutionDataVisitor(dumpCallback)
			executionDataReader.setSessionInfoVisitor(dumpCallback)
			executionDataReader.read()
			dumpCallback.processDump() // Ensure that the last read dump is also consumed
		}
	}

	/** Collects execution information per session and passes it to the consumer . */
	private class DumpCallback(private val dumpConsumer: Consumer<Dump>) : IExecutionDataVisitor, ISessionInfoVisitor {

		/** The dump that is currently being read. */
		private var currentDump: Dump? = null

		/** The store to which coverage is currently written to. */
		private var store: ExecutionDataStore? = null

		override fun visitSessionInfo(info: SessionInfo) {
			processDump()
			store = ExecutionDataStore()
			currentDump = Dump(info, store!!)
		}

		override fun visitClassExecution(data: ExecutionData) {
			store?.put(data)
		}

		fun processDump() {
			currentDump?.let {
				dumpConsumer.accept(it)
				currentDump = null
			}
		}
	}
}
