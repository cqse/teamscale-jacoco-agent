package com.teamscale.report.testwise.jacoco

import com.teamscale.report.jacoco.dump.Dump
import com.teamscale.report.testwise.jacoco.cache.AnalyzerCache
import com.teamscale.report.testwise.jacoco.cache.CoverageGenerationException
import com.teamscale.report.testwise.jacoco.cache.ProbesCache
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder
import com.teamscale.report.util.ILogger
import org.jacoco.core.data.ExecutionDataStore
import java.io.File
import java.io.IOException
import java.util.function.Predicate

/**
 * Helper class for analyzing class files, reading execution data and converting them to coverage data.
 */
internal class CachingExecutionDataReader
/** Constructor.  */
    (
    /** The logger.  */
    private val logger: ILogger
) {

    /** Cached probes.  */
    private var probesCache: ProbesCache? = null

    /**
     * Analyzes the given class/jar/war/... files and creates a lookup of which probes belong to which method.
     */
    @Throws(CoverageGenerationException::class)
    fun analyzeClassDirs(
        classesDirectories: Collection<File>,
        locationIncludeFilter: Predicate<String>,
        ignoreNonidenticalDuplicateClassFiles: Boolean
    ) {
        if (probesCache != null) {
            return
        }
        probesCache = ProbesCache(logger, ignoreNonidenticalDuplicateClassFiles)
        val analyzer = AnalyzerCache(probesCache, locationIncludeFilter, logger)
        for (classDir in classesDirectories) {
            if (classDir.exists()) {
                try {
                    analyzer.analyzeAll(classDir)
                } catch (e: IOException) {
                    logger.error(
                        "Failed to analyze class files in " + classDir + "! " +
                                "Maybe the folder contains incompatible class files. " +
                                "Coverage for class files in this folder will be ignored.", e
                    )
                }

            }
        }
        if (probesCache!!.isEmpty) {
            val directoryList = classesDirectories.joinToString(",") { it.path }
            throw CoverageGenerationException("No class files found in the given directories! $directoryList")
        }
    }

    /**
     * Converts the given store to coverage data. The coverage will only contain line range coverage information.
     */
    fun buildCoverage(dumps: List<Dump>): TestwiseCoverage {
        val testwiseCoverage = TestwiseCoverage()
        for (dump in dumps) {
            val testId = dump.info.id
            if (testId.isEmpty()) {
                // Ignore intermediate coverage that does not belong to any specific test
                logger.debug("Found a session with empty name! This could indicate that coverage is dumped also for " + "coverage in between tests or that the given test name was empty")
                continue
            }
            try {
                val testCoverage = buildCoverage(testId, dump.store)
                testwiseCoverage.add(testCoverage)
            } catch (e: CoverageGenerationException) {
                logger.error("Failed to generate coverage for test $testId! Skipping to the next test.", e)
            }

        }
        return testwiseCoverage
    }

    /**
     * Converts the given store to coverage data. The coverage will only contain line range coverage information.
     */
    @Throws(CoverageGenerationException::class)
    private fun buildCoverage(testId: String, executionDataStore: ExecutionDataStore): TestCoverageBuilder {
        val testCoverage = TestCoverageBuilder(testId)
        for (executionData in executionDataStore.contents) {
            testCoverage.add(probesCache!!.getCoverage(executionData))
        }
        return testCoverage
    }
}