package org.jacoco.core.internal.analysis;

import com.teamscale.report.testwise.jacoco.cache.ClassCoverageLookup;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

/**
 * Analyzes a class to reconstruct probe information.
 * <p>
 * A probe lookup holds for a single class which probe belongs to which lines. The actual filling of the
 * {@link ClassCoverageLookup} happens in {@link CachingInstructionsBuilder}.
 */
public class CachingClassAnalyzer extends ClassAnalyzer {

	/** The cache, which contains a probe lookups for the current class. */
	private final ClassCoverageLookup classCoverageLookup;

	/**
	 * Creates a new analyzer that builds coverage data for a class.
	 *
	 * @param classCoverageLookup cache for the class' probes
	 * @param coverage            coverage node for the analyzed class data
	 * @param stringPool          shared pool to minimize the number of {@link String} instances
	 */
	public CachingClassAnalyzer(ClassCoverageLookup classCoverageLookup, ClassCoverageImpl coverage, StringPool stringPool) {
		super(coverage, null, stringPool);
		this.classCoverageLookup = classCoverageLookup;
	}

	@Override
	public void visitSource(String source, String debug) {
		super.visitSource(source, debug);
		classCoverageLookup.setSourceFileName(source);
	}

	@Override
	public MethodProbesVisitor visitMethod(final int access, final String name,
										   final String desc, final String signature, final String[] exceptions) {
		final CachingInstructionsBuilder builder = new CachingInstructionsBuilder(classCoverageLookup);

		return new MethodAnalyzer(builder) {

			@Override
			public void accept(final MethodNode methodNode,
							   final MethodVisitor methodVisitor) {
				super.accept(methodNode, methodVisitor);
				// CHANGE The regular ClassAnalyzer gets the coverage here, we instead
				// fill the cache with the obtained information
				builder.fillCache();
			}
		};
	}

	@Override
	public void visitTotalProbeCount(final int count) {
		classCoverageLookup.setTotalProbeCount(count);
	}
}
