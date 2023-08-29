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
/* package */ public class FilteringAnalyzer extends OpenAnalyzer {

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
			logger.debug("Excluding class file " + location);
			return 1;
		}
		if (location.endsWith(".jar")) {
			return analyzeJar(input, location);
		}
		return super.analyzeAll(input, location);
	}

	@Override
	public void analyzeClass(final byte[] buffer, final String location)
			throws IOException {
		try {
			analyzeClass(buffer);
		} catch (final RuntimeException cause) {
			if (isUnsupportedClassFile(cause)) {
				logger.error(cause.getMessage() + " in " + location);
			} else {
				throw analyzerError(location, cause);
			}
		}
	}

	/**
	 * Checks if the error indicates that the class file might be newer than what is currently supported by
	 * JaCoCo. The concrete error message seems to depend on the used JVM, so we only check for "Unsupported" which seems
	 * to be common amongst all of them.
	 */
	private boolean isUnsupportedClassFile(RuntimeException cause) {
		return cause instanceof IllegalArgumentException && cause.getMessage()
				.startsWith("Unsupported");
	}

	/**
	 * Copied from Analyzer.analyzeZip renamed to analyzeJar and added wrapping BashFileSkippingInputStream.
	 */
	protected int analyzeJar(final InputStream input, final String location)
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
