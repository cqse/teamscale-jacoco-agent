/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco;

import com.teamscale.report.util.BashFileSkippingInputStream;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.ICoverageVisitor;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
		if (location.endsWith(".jar")) {
			return analyzeJar(input, location);
		}
		return super.analyzeAll(input, location);
	}

	/** Copied from Analyzer.analyzerError */
	private IOException analyzerError(final String location,
									  final Exception cause) {
		final IOException ex = new IOException(
				String.format("Error while analyzing %s.", location));
		ex.initCause(cause);
		return ex;
	}

	/**
	 * Copied from Analyzer.analyzeZip renamed to analyzeJar and
	 * added wrapping BashFileSkippingInputStream.
	 */
	private int analyzeJar(final InputStream input, final String location)
			throws IOException {
		ZipInputStream zip = new ZipInputStream(new BashFileSkippingInputStream(input));
		ZipEntry entry;
		int count = 0;
		while ((entry = nextEntry(zip, location)) != null) {
			count += analyzeAll(zip, location + "@" + entry.getName());
		}
		return count;
	}

	/** Copied from Analyzer.nextEntry. */
	private ZipEntry nextEntry(final ZipInputStream input,
							   final String location) throws IOException {
		try {
			return input.getNextEntry();
		} catch (final IOException e) {
			throw analyzerError(location, e);
		}
	}
}