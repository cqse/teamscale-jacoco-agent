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
class DuplicateIgnoringCoverageBuilder extends CoverageBuilder {

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
			logger.warn("Ignoring duplicate, non-identical class file for class {}", coverage.getName(), e);
		}
	}

}