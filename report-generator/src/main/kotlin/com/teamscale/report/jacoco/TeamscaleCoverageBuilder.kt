/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco

import com.teamscale.report.EDuplicateClassFileBehavior
import com.teamscale.report.util.ILogger
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IClassCoverage
import org.jacoco.core.analysis.ICounter
import org.jacoco.core.internal.analysis.BundleCoverageImpl

/**
 * Modified [CoverageBuilder] can ignore non-identical duplicate classes or classes without coverage. In addition,
 * coverage returned via [.getBundle] will only return source file coverage because Teamscale does not
 * need class coverage anyway. This reduces XML size by approximately half.
 *
 * @param logger The logger.
 * @param duplicateClassFileBehavior How to behave if duplicate class files are encountered.
 * @param ignoreUncoveredClasses Whether to ignore uncovered classes (i.e. leave them out of the report).
 */
internal class TeamscaleCoverageBuilder(
	private val logger: ILogger,
	private val duplicateClassFileBehavior: EDuplicateClassFileBehavior,
	private val ignoreUncoveredClasses: Boolean
) : CoverageBuilder() {
	/** Just returns source file coverage, because Teamscale does not need class coverage.  */
	override fun getBundle(name: String) =
		BundleCoverageImpl(name, emptyList(), sourceFiles)

	/** {@inheritDoc}  */
	override fun visitCoverage(coverage: IClassCoverage) {
		if (ignoreUncoveredClasses && (coverage.classCounter.status and ICounter.FULLY_COVERED) == 0) {
			return
		}

		try {
			super.visitCoverage(coverage)
		} catch (e: IllegalStateException) {
			when (duplicateClassFileBehavior) {
				EDuplicateClassFileBehavior.IGNORE -> return
				EDuplicateClassFileBehavior.WARN -> {
					// we do not log the exception here as it does not provide additional valuable information
					// and may confuse users into thinking there's a serious
					// problem with the agent due to the stack traces in the log
					logger.warn(
						"Ignoring duplicate, non-identical class file for class ${coverage.name} compiled " +
								"from source file ${coverage.sourceFileName}. This happens when a class with the same " +
								"fully-qualified name is loaded twice but the two loaded class files are not identical. " +
								"A common reason for this is that the same library or shared code is included twice in " +
								"your application but in two different versions. The produced coverage for this class " +
								"may not be accurate or may even be unusable. To fix this problem, please resolve the " +
								"conflict between both class files in your application."
					)
					return
				}

				else -> throw e
			}
		}
	}
}
