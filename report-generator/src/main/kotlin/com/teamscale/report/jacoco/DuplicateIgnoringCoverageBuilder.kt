/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco

import com.teamscale.report.util.ILogger
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IClassCoverage

/** Modified [CoverageBuilder] that ignores non-identical duplicates.  */
/* package */internal class DuplicateIgnoringCoverageBuilder(
    /** The logger.  */
    private val logger: ILogger
) : CoverageBuilder() {

    /** {@inheritDoc}  */
    override fun visitCoverage(coverage: IClassCoverage) {
        try {
            super.visitCoverage(coverage)
        } catch (e: IllegalStateException) {
            logger.warn(
                "Ignoring duplicate, non-identical class file for class " + coverage
                    .name + " compiled from source file " + coverage.sourceFileName + "."
                        + " This happens when a class with the same fully-qualified name is loaded twice but the two loaded class files are not identical."
                        + " A common reason for this is that the same library or shared code is included twice in your application but in two different versions."
                        + " The produced coverage for this class may not be accurate or may even be unusable."
                        + " To fix this problem, please resolve the conflict between both class files in your application.",
                e
            )
        }

    }

}