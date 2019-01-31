package com.teamscale.report.testwise.jacoco.cache

import com.teamscale.report.testwise.model.builder.FileCoverageBuilder
import com.teamscale.report.util.ILogger
import org.jacoco.core.data.ExecutionData

import java.util.HashMap
import java.util.HashSet

/**
 * Holds [ClassCoverageLookup]s for all analyzed classes.
 */
class ProbesCache
/** Constructor.  */
    (
    /** The logger.  */
    private val logger: ILogger,
    /** Whether to ignore non-identical duplicates of class files.  */
    private val ignoreNonidenticalDuplicateClassFiles: Boolean
) {

    /** A mapping from class ID (CRC64 of the class file) to [ClassCoverageLookup].  */
    private val classCoverageLookups = HashMap<Long, ClassCoverageLookup>()

    /** Holds all fully-qualified class names that are already contained in the cache.  */
    private val containedClasses = HashSet<String>()

    /** Returns true if the cache does not contains coverage for any class.  */
    val isEmpty: Boolean
        get() = classCoverageLookups.isEmpty()

    /** Adds a new class entry to the cache and returns its [ClassCoverageLookup].  */
    fun createClass(classId: Long, className: String): ClassCoverageLookup {
        if (containedClasses.contains(className)) {
            logger.warn(
                "Non-identical class file for class " + className + "."
                        + " This happens when a class with the same fully-qualified name is loaded twice but the two loaded class files are not identical."
                        + " A common reason for this is that the same library or shared code is included twice in your application but in two different versions."
                        + " The produced coverage for this class may not be accurate or may even be unusable."
                        + " To fix this problem, please resolve the conflict between both class files in your application."
            )
            if (!ignoreNonidenticalDuplicateClassFiles) {
                throw IllegalStateException(
                    "Found non-identical class file for class $className. See logs for more details."
                )
            }
        }
        containedClasses.add(className)
        val classCoverageLookup = ClassCoverageLookup(className)
        classCoverageLookups[classId] = classCoverageLookup
        return classCoverageLookup
    }

    /** Returns whether a class with the given class ID has already been analyzed.  */
    fun containsClassId(classId: Long): Boolean {
        return classCoverageLookups.containsKey(classId)
    }

    /**
     * Converts the given [ExecutionData] to [FileCoverageBuilder] using the cached lookups or null if the class
     * file of this class has not been included in the analysis or was not covered.
     */
    @Throws(CoverageGenerationException::class)
    fun getCoverage(executionData: ExecutionData): FileCoverageBuilder? {
        val classId = executionData.id
        if (!containsClassId(classId)) {
            logger.debug(
                "Found coverage for a class " + executionData
                    .name + " that was not provided. Either you did not provide " +
                        "all relevant class files or you did not adjust the include/exclude filters on the agent to exclude " +
                        "coverage from irrelevant code."
            )
            return null
        }
        return if (!executionData.hasHits()) {
            null
        } else classCoverageLookups[classId]?.getFileCoverage(executionData, logger)

    }
}
