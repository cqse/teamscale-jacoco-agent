/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco;

import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.ICoverageVisitor;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;

/**
 * {@link Analyzer} that filters the analyzed class files based on a {@link Predicate}.
 */
/* package */ public class FilteringAnalyzer extends Analyzer {

	/** The filter for the analyzed class files. */
	private final ClasspathWildcardIncludeFilter locationIncludeFilter;
	private final ILogger logger;

	public FilteringAnalyzer(ExecutionDataStore executionData, ICoverageVisitor coverageVisitor,
							 ClasspathWildcardIncludeFilter locationIncludeFilter, ILogger logger) {
		super(executionData, coverageVisitor);
		this.locationIncludeFilter = locationIncludeFilter;
		this.logger = logger;
	}

	/** {@inheritDoc} */
	@Override
	public int analyzeAll(InputStream input, String location) throws IOException {
		if (location.endsWith(".class") && !locationIncludeFilter.isIncluded(location)) {
			logger.debug("Filtering class file " + location);
			return 0;
		}
		return super.analyzeAll(input, location);
	}
}