package com.teamscale.report.testwise.jacoco.cache;

import com.teamscale.report.jacoco.FilteringAnalyzer;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.internal.analysis.CachingClassAnalyzer;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.jacoco.core.internal.analysis.StringPool;
import org.jacoco.core.internal.data.CRC64;
import org.jacoco.core.internal.flow.ClassProbesAdapter;
import org.jacoco.core.internal.instr.InstrSupport;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * An {@link AnalyzerCache} instance processes a set of Java class/jar/war/... files and builds a {@link
 * ClassCoverageLookup} for each of the classes.
 * <p>
 * For every class that gets found {@link #analyzeClass(byte[])} is called. A class is identified by its class ID which
 * is a CRC64 checksum of the classfile. We process each class with {@link CachingClassAnalyzer} to fill a {@link
 * ClassCoverageLookup}.
 */
public class AnalyzerCache extends FilteringAnalyzer {

	/** The probes cache. */
	private final ProbesCache probesCache;


	private final StringPool stringPool = new StringPool();

	/** Creates a new analyzer filling the given cache. */
	public AnalyzerCache(ProbesCache probesCache, ClasspathWildcardIncludeFilter locationIncludeFilter,
						 ILogger logger) {
		super(null, null, locationIncludeFilter, logger);
		this.probesCache = probesCache;
	}

	/**
	 * Analyses the given class. Instead of the original implementation in {@link Analyzer#analyzeClass(byte[])} we
	 * don't use concrete execution data, but instead build a probe cache to speed up repeated lookups.
	 */
	@Override
	protected void analyzeClass(final byte[] source) {
		long classId = CRC64.classId(source);
		if (probesCache.containsClassId(classId)) {
			return;
		}
		final ClassReader reader = InstrSupport.classReaderFor(source);
		ClassCoverageLookup classCoverageLookup = probesCache.createClass(classId, reader.getClassName());

		// Dummy class coverage object that allows us to subclass ClassAnalyzer with CachingClassAnalyzer and reuse its
		// IFilterContext implementation
		final ClassCoverageImpl dummyClassCoverage = new ClassCoverageImpl(reader.getClassName(),
				classId, false);

		CachingClassAnalyzer classAnalyzer = new CachingClassAnalyzer(classCoverageLookup, dummyClassCoverage,
				stringPool);
		final ClassVisitor visitor = new ClassProbesAdapter(classAnalyzer, false);
		reader.accept(visitor, 0);
	}

	/**
	 * Adds caching for jar files to the analyze jar functionality.
	 */
	@Override
	protected int analyzeJar(final InputStream input, final String location) throws IOException {
		long jarId = CRC64.classId(Files.readAllBytes(Paths.get(location)));
		int probesCountForJarId = probesCache.countForJarId(jarId);
		if (probesCountForJarId != 0) {
			return probesCountForJarId;
		}
		int count = super.analyzeJar(input, location);
		probesCache.addJarId(jarId, count);
		return count;
	}
}
