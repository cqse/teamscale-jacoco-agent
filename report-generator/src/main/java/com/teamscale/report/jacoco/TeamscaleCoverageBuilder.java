/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco;

import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.internal.analysis.BundleCoverageImpl;

import java.util.Collections;

/**
 * Modified {@link CoverageBuilder} can ignore non-identical duplicate classes or classes without coverage. In addition,
 * coverage returned via {@link #getBundle(String)} will only return source file coverage because Teamscale does not
 * need class coverage anyway. This reduces XML size by approximately half.
 */
/* package */class TeamscaleCoverageBuilder extends CoverageBuilder {

	/** The logger. */
	private final ILogger logger;

	/** How to behave if duplicate class files are encountered. */
	private final EDuplicateClassFileBehavior duplicateClassFileBehavior;

	/** Whether to ignore uncovered classes (i.e. leave them out of the report). */
	private final boolean ignoreUncoveredClasses;

	TeamscaleCoverageBuilder(ILogger logger, EDuplicateClassFileBehavior duplicateClassFileBehavior,
							 boolean removeUncoveredClasses) {
		this.logger = logger;
		this.duplicateClassFileBehavior = duplicateClassFileBehavior;
		this.ignoreUncoveredClasses = removeUncoveredClasses;
	}

	/** Just returns source file coverage, because Teamscale does not need class coverage. */
	@Override
	public IBundleCoverage getBundle(final String name) {
		return new BundleCoverageImpl(name, Collections.emptyList(), getSourceFiles());
	}

	/** {@inheritDoc} */
	@Override
	public void visitCoverage(IClassCoverage coverage) {
		if (ignoreUncoveredClasses && (coverage.getClassCounter().getStatus() & ICounter.FULLY_COVERED) == 0) {
			return;
		}

		try {
			super.visitCoverage(coverage);
		} catch (IllegalStateException e) {
			switch (duplicateClassFileBehavior) {
				case IGNORE:
					return;
				case WARN:
					// we deliberately do not log the exception in this case as it does not provide any additional
					// valuable information but confuses users into thinking there's a serious problem with the agent
					// as they only see that there are stack traces in the log
					logger.warn("Ignoring duplicate, non-identical class file for class " + coverage
							.getName() + " compiled from source file " + coverage.getSourceFileName() + "."
							+ " This happens when a class with the same fully-qualified name is loaded twice but the two loaded class files are not identical."
							+ " A common reason for this is that the same library or shared code is included twice in your application but in two different versions."
							+ " The produced coverage for this class may not be accurate or may even be unusable."
							+ " To fix this problem, please resolve the conflict between both class files in your application.");
					return;
				default:
					throw e;
			}
		}
	}
}
