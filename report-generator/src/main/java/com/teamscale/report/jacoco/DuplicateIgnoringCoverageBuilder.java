/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco;

import com.teamscale.report.util.ILogger;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;

/** Modified {@link CoverageBuilder} that ignores non-identical duplicates. */
/* package */class DuplicateIgnoringCoverageBuilder extends CoverageBuilder {

	/** The logger. */
	private final ILogger logger;

	/** Whether to warn on duplicate class files. */
	private final boolean warnOnDuplicateClassFile;

	DuplicateIgnoringCoverageBuilder(ILogger logger, boolean warnOnDuplicateClassFile) {
		this.logger = logger;
		this.warnOnDuplicateClassFile = warnOnDuplicateClassFile;
	}

	/** {@inheritDoc} */
	@Override
	public void visitCoverage(IClassCoverage coverage) {
		try {
			super.visitCoverage(coverage);
		} catch (IllegalStateException e) {
			if (warnOnDuplicateClassFile) {
				logger.warn("Ignoring duplicate, non-identical class file for class " + coverage
								.getName() + " compiled from source file " + coverage.getSourceFileName() + "."
								+ " This happens when a class with the same fully-qualified name is loaded twice but the two loaded class files are not identical."
								+ " A common reason for this is that the same library or shared code is included twice in your application but in two different versions."
								+ " The produced coverage for this class may not be accurate or may even be unusable."
								+ " To fix this problem, please resolve the conflict between both class files in your application.",
						e);
			}
		}
	}

}