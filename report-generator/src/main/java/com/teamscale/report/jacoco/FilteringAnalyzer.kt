/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco

import com.teamscale.report.util.ILogger
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.data.ExecutionDataStore

import java.io.IOException
import java.io.InputStream
import java.util.function.Predicate

/**
 * [Analyzer] that filters the analyzed class files based on a
 * [Predicate].
 */
/* package */ open class FilteringAnalyzer
/** Constructor.  */
    (
    executionData: ExecutionDataStore, coverageVisitor: ICoverageVisitor,
    /** The filter for the analyzed class files.  */
    private val locationIncludeFilter: Predicate<String>,
    /** The logger.  */
    private val logger: ILogger
) : Analyzer(executionData, coverageVisitor) {

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun analyzeAll(input: InputStream, location: String): Int {
        if (location.endsWith(".class") && !locationIncludeFilter.test(location)) {
            logger.debug("Filtering class file $location")
            return 0
        }
        return super.analyzeAll(input, location)
    }
}