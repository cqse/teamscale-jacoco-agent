package eu.cqse.teamscale.jacoco.analysis;

import eu.cqse.teamscale.jacoco.cache.ProbeLookup;
import org.jacoco.core.internal.flow.IFrame;
import org.jacoco.core.internal.flow.LabelInfo;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.objectweb.asm.Label;

/**
 * A {@link MethodProbesVisitor} that analyzes which statements and branches of
 * a method have been executed based on given probe data.
 * <p>
 * It's core is a copy of {@link org.jacoco.core.internal.analysis.MethodAnalyzer} that has been
 * extended with caching functionality to speed up report generation.
 */
public class CachingMethodAnalyzer extends MethodProbesVisitor {

	private final ProbeLookup probeLookup;

	/**
	 * New Method analyzer for the given probe data.
	 *
	 * @param probeLookup cache of the class' probes
	 */
	CachingMethodAnalyzer(ProbeLookup probeLookup) {
		this.probeLookup = probeLookup;
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		probeLookup.visitLine(line);
	}

	@Override
	public void visitProbe(final int probeId) {
		addProbe(probeId);
	}

	@Override
	public void visitJumpInsnWithProbe(final int opcode, final Label label,
									   final int probeId, final IFrame frame) {
		addProbe(probeId);
	}

	@Override
	public void visitInsnWithProbe(final int opcode, final int probeId) {
		addProbe(probeId);
	}


	@Override
	public void visitTableSwitchInsnWithProbes(final int min, final int max,
											   final Label dflt, final Label[] labels, final IFrame frame) {
		visitSwitchInsnWithProbes(dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsnWithProbes(final Label dflt,
												final int[] keys, final Label[] labels, final IFrame frame) {
		visitSwitchInsnWithProbes(dflt, labels);
	}

	private void visitSwitchInsnWithProbes(final Label dflt,
										   final Label[] labels) {
		LabelInfo.resetDone(dflt);
		LabelInfo.resetDone(labels);
		visitSwitchTarget(dflt);
		for (final Label l: labels) {
			visitSwitchTarget(l);
		}
	}

	private void visitSwitchTarget(final Label label) {
		final int id = LabelInfo.getProbeId(label);
		if (!LabelInfo.isDone(label)) {
			if (id != LabelInfo.NO_PROBE) {
				addProbe(id);
			}
		}
	}

	@Override
	public void visitEnd() {
		probeLookup.finishMethod();
	}

	private void addProbe(final int probeId) {
		probeLookup.addProbe(probeId);
	}
}
