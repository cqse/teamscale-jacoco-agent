package com.teamscale.report.testwise.jacoco.analysis;

import com.teamscale.report.testwise.jacoco.cache.ClassCoverageLookup;
import org.jacoco.core.internal.flow.ClassProbesVisitor;
import org.jacoco.core.internal.flow.MethodProbesVisitor;

/**
 * Analyzes a class to reconstruct probe information.
 * <p>
 * It's core is a copy of {@link org.jacoco.core.internal.analysis.ClassAnalyzer} that has been
 * changed to support caching functionality.
 * <p>
 * A probe lookup holds for a single class which probe belongs to which method (line range). The actual filling of the
 * {@link ClassCoverageLookup} happens in {@link CachingMethodAnalyzer}.
 */
public class CachingClassAnalyzer extends ClassProbesVisitor {

	/** The cache, which contains a probe lookups for the current class. */
	private final ClassCoverageLookup classCoverageLookup;

	/**
	 * Creates a new analyzer that builds coverage data for a class.
	 *
	 * @param classCoverageLookup cache for the class' probes
	 */
	public CachingClassAnalyzer(ClassCoverageLookup classCoverageLookup) {
		this.classCoverageLookup = classCoverageLookup;
	}

	@Override
	public void visitSource(String source, String debug) {
		classCoverageLookup.setSourceFileName(source);
	}

	@Override
	public MethodProbesVisitor visitMethod(final int access, final String name,
										   final String desc, final String signature, final String[] exceptions) {
		return new CachingMethodAnalyzer(classCoverageLookup);
	}

	@Override
	public void visitTotalProbeCount(final int count) {
		classCoverageLookup.setTotalProbeCount(count);
	}
}
