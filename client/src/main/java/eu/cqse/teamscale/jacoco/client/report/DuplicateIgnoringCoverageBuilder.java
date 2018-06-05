/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.report;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;

/** Modified {@link CoverageBuilder} that ignores non-identical duplicates. */
/* package */class DuplicateIgnoringCoverageBuilder extends CoverageBuilder {

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visitCoverage(IClassCoverage coverage) {
		try {
			super.visitCoverage(coverage);
		} catch (IllegalStateException e) {
			logger.warn("Ignoring duplicate, non-identical class file for class {} compiled from source file {}."
					+ " This happens when a class with the same fully-qualified name is loaded twice but the two loaded class files are not identical."
					+ " A common reason for this is that the same library or shared code is included twice in your application but in two different versions."
					+ " The produced coverage for this class may not be accurate or may even be unusable."
					+ " To fix this problem, please resolve the conflict between both class files in your application.",
					coverage.getName(), coverage.getSourceFileName(), e);
		}
	}

}