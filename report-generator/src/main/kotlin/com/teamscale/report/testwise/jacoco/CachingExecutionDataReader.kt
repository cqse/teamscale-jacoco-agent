package com.teamscale.report.testwise.jacoco

import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.testwise.jacoco.cache.AnalyzerCache
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException
import com.teamscale.report.testwise.jacoco.cache.ProbesCache
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.ILogger
import org.jacoco.core.data.ExecutionDataStore
import java.io.File
import java.io.IOException
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * Helper class for analyzing class files, reading execution data and converting them to coverage data.
 */
open class CachingExecutionDataReader(
	private val logger: ILogger, private val classesDirectories: Collection<File>,
	private val locationIncludeFilter: ClasspathWildcardIncludeFilter,
	private val duplicateClassFileBehavior: EDuplicateClassFileBehavior
) {
	private var probesCache: ProbesCache? = null

	/**
	 * Analyzes the class/jar/war/... files and creates a lookup of which probes belong to which method.
	 */
	fun analyzeClassDirs() {
		if (probesCache == null) {
			probesCache = ProbesCache(logger, duplicateClassFileBehavior)
		}
		if (classesDirectories.isEmpty()) {
			logger.warn("No class directories found for caching.")
			return
		}
		val analyzer = AnalyzerCache(
			probesCache!!,
			locationIncludeFilter,
			logger
		)
		var classCount = 0
		for (classDir in classesDirectories) {
			if (classDir.exists()) {
				try {
					classCount += analyzer.analyzeAll(classDir)
				} catch (e: IOException) {
					logger.error(
						"Failed to analyze class files in " + classDir + "! " +
								"Maybe the folder contains incompatible class files. " +
								"Coverage for class files in this folder will be ignored.", e
					)
				}
			}
		}
		if (classCount == 0) {
			val directoryList: String = classesDirectories.stream().map { obj: File -> obj.path }.collect(
				Collectors.joining(",")
			)
			logger.error("No class files found in the given directories! $directoryList")
		} else if (probesCache?.isEmpty == true) {
			val directoryList: String = classesDirectories.stream().map { obj: File -> obj.path }.collect(
				Collectors.joining(",")
			)
			logger.error(
				"None of the $classCount class files found in the given directories match the configured include/exclude patterns! $directoryList"
			)
		}
	}

	/**
	 * Converts the given store to coverage data. The coverage will only contain line range coverage information.
	 */
	fun buildCoverageConsumer(
		locationIncludeFilter: ClasspathWildcardIncludeFilter,
		nextConsumer: Consumer<TestCoverageBuilder>
	) = DumpConsumer(logger, locationIncludeFilter, nextConsumer)

	/**
	 * Consumer of [Dump] objects. Converts them to [TestCoverageBuilder] and passes them to the
	 * nextConsumer.
	 */
	inner class DumpConsumer(
		/** The logger.  */
		private val logger: ILogger,
		/** The location include filter to be applied on the profiled classes.  */
		private val locationIncludeFilter: ClasspathWildcardIncludeFilter,
		/** Consumer that should be called with the newly built TestCoverageBuilder.  */
		private val nextConsumer: Consumer<TestCoverageBuilder>
	) : Consumer<Dump> {
		override fun accept(dump: Dump) {
			val testId = dump.info.id
			if (testId.isEmpty()) {
				// Ignore intermediate coverage that does not belong to any specific test
				logger.debug(
					"Found a session with empty name! This could indicate that coverage is dumped also for " +
							"coverage in between tests or that the given test name was empty!"
				)
				return
			}
			try {
				val testCoverage: TestCoverageBuilder = buildCoverage(
					testId, dump.store,
					locationIncludeFilter
				)
				nextConsumer.accept(testCoverage)
			} catch (e: CoverageGenerationException) {
				logger.error("Failed to generate coverage for test $testId! Skipping to the next test.", e)
			}
		}

		/**
		 * Converts the given store to coverage data. The coverage will only contain line range coverage information.
		 */
		@Throws(CoverageGenerationException::class)
		private fun buildCoverage(
			testId: String, executionDataStore: ExecutionDataStore,
			locationIncludeFilter: ClasspathWildcardIncludeFilter
		): TestCoverageBuilder {
			val testCoverage = TestCoverageBuilder(testId)
			for (executionData in executionDataStore.contents) {
				testCoverage.add(probesCache!!.getCoverage(executionData, locationIncludeFilter))
			}
			probesCache!!.flushLogger()
			return testCoverage
		}
	}
}
