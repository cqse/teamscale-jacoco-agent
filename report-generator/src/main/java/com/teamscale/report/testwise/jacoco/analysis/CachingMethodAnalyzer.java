package com.teamscale.report.testwise.jacoco.analysis;

import com.teamscale.report.testwise.jacoco.cache.ClassCoverageLookup;
import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.internal.analysis.filter.IFilterContext;
import org.jacoco.core.internal.analysis.filter.IFilterOutput;
import org.jacoco.core.internal.flow.IFrame;
import org.jacoco.core.internal.flow.Instruction;
import org.jacoco.core.internal.flow.LabelInfo;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link MethodProbesVisitor} that analyzes which statements and branches of
 * a method have been executed based on given probe data.
 * <p>
 * It's core is a copy of {@link org.jacoco.core.internal.analysis.MethodAnalyzer} that has been
 * extended with caching functionality to speed up report generation.
 * <p>
 * This class contains callbacks for stepping through a method at bytecode level which has been
 * decorated with probes by JaCoCo in a depth-first-search like way.
 * <p>
 * Changes that have been applied to the original class are marked with ADDED and REMOVED comments to make it as easy
 * as possible to adjust the implementation to new versions of JaCoCo.
 *
 * When updating JaCoCo make a diff of the previous {@link org.jacoco.core.internal.analysis.MethodAnalyzer}
 * implementation and the new implementation and update this class accordingly.
 */
public class CachingMethodAnalyzer extends MethodProbesVisitor implements IFilterOutput {

	// REMOVED private final boolean[] probes;

	// ADDED field to hold a reference to our coverage lookup
	private final ClassCoverageLookup classCoverageLookup;

	private final IFilter filter;

	private final IFilterContext filterContext;

	// REMOVED private final MethodCoverageImpl coverage;

	private int currentLine = ISourceNode.UNKNOWN_LINE;

	private int firstLine = ISourceNode.UNKNOWN_LINE;

	private int lastLine = ISourceNode.UNKNOWN_LINE;

	// Due to ASM issue #315745 there can be more than one label per instruction
	private final List<Label> currentLabel = new ArrayList<Label>(2);

	/** List of all analyzed instructions */
	private final List<Instruction> instructions = new ArrayList<Instruction>();

	/** List of all predecessors of covered probes */
	private final List<CoveredProbe> coveredProbes = new ArrayList<CoveredProbe>();

	/** List of all jumps encountered */
	private final List<Jump> jumps = new ArrayList<Jump>();

	/** Last instruction in byte code sequence */
	private Instruction lastInsn;

	/**
	 * New Method analyzer for the given probe data.
	 * <p>
	 * ADDED ClassCoverageLookup classCoverageLookup parameter
	 * REMOVED final String name, final String desc, final String signature, final boolean[] probes
	 *
	 * @param classCoverageLookup cache of the class' probes
	 * @param filter              filter which should be applied
	 * @param filterContext       class context information for the filter
	 */
	CachingMethodAnalyzer(ClassCoverageLookup classCoverageLookup,
						  final IFilter filter,
						  final IFilterContext filterContext) {
		this.classCoverageLookup = classCoverageLookup;
		this.filter = filter;
		this.filterContext = filterContext;
	}

	/**
	 * {@link MethodNode#accept(MethodVisitor)}
	 */
	@Override
	public void accept(final MethodNode methodNode,
					   final MethodVisitor methodVisitor) {
		filter.filter(methodNode, filterContext, this);

		methodVisitor.visitCode();
		for (final TryCatchBlockNode n : methodNode.tryCatchBlocks) {
			n.accept(methodVisitor);
		}
		currentNode = methodNode.instructions.getFirst();
		while (currentNode != null) {
			currentNode.accept(methodVisitor);
			currentNode = currentNode.getNext();
		}
		methodVisitor.visitEnd();
	}

	private final Set<AbstractInsnNode> ignored = new HashSet<AbstractInsnNode>();

	/**
	 * Instructions that should be merged form disjoint sets. Coverage
	 * information from instructions of one set will be merged into
	 * representative instruction of set.
	 * <p>
	 * Each such set is represented as a singly linked list: each element except
	 * one references another element from the same set, element without
	 * reference - is a representative of this set.
	 * <p>
	 * This map stores reference (value) for elements of sets (key).
	 */
	private final Map<AbstractInsnNode, AbstractInsnNode> merged = new HashMap<AbstractInsnNode, AbstractInsnNode>();

	private final Map<AbstractInsnNode, Instruction> nodeToInstruction = new HashMap<AbstractInsnNode, Instruction>();

	private AbstractInsnNode currentNode;

	public void ignore(final AbstractInsnNode fromInclusive,
					   final AbstractInsnNode toInclusive) {
		for (AbstractInsnNode i = fromInclusive; i != toInclusive; i = i
				.getNext()) {
			ignored.add(i);
		}
		ignored.add(toInclusive);
	}

	private AbstractInsnNode findRepresentative(AbstractInsnNode i) {
		AbstractInsnNode r = merged.get(i);
		while (r != null) {
			i = r;
			r = merged.get(i);
		}
		return i;
	}

	public void merge(AbstractInsnNode i1, AbstractInsnNode i2) {
		i1 = findRepresentative(i1);
		i2 = findRepresentative(i2);
		if (i1 != i2) {
			merged.put(i2, i1);
		}
	}

	private final Map<AbstractInsnNode, Set<AbstractInsnNode>> replacements = new HashMap<AbstractInsnNode, Set<AbstractInsnNode>>();

	public void replaceBranches(final AbstractInsnNode source,
								final Set<AbstractInsnNode> newTargets) {
		replacements.put(source, newTargets);
	}

	@Override
	public void visitLabel(final Label label) {
		currentLabel.add(label);
		if (!LabelInfo.isSuccessor(label)) {
			lastInsn = null;
		}
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		currentLine = line;
		if (firstLine > line || lastLine == ISourceNode.UNKNOWN_LINE) {
			firstLine = line;
		}
		if (lastLine < line) {
			lastLine = line;
		}
	}

	private void visitInsn() {
		final Instruction insn = new Instruction(currentNode, currentLine);
		nodeToInstruction.put(currentNode, insn);
		instructions.add(insn);
		if (lastInsn != null) {
			insn.setPredecessor(lastInsn, 0);
		}
		final int labelCount = currentLabel.size();
		if (labelCount > 0) {
			for (int i = labelCount; --i >= 0; ) {
				LabelInfo.setInstruction(currentLabel.get(i), insn);
			}
			currentLabel.clear();
		}
		lastInsn = insn;
	}

	@Override
	public void visitInsn(final int opcode) {
		visitInsn();
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		visitInsn();
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		visitInsn();
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		visitInsn();
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner,
							   final String name, final String desc) {
		visitInsn();
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner,
								final String name, final String desc, final boolean itf) {
		visitInsn();
	}

	@Override
	public void visitInvokeDynamicInsn(final String name, final String desc,
									   final Handle bsm, final Object... bsmArgs) {
		visitInsn();
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		visitInsn();
		jumps.add(new Jump(lastInsn, label, 1));
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		visitInsn();
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		visitInsn();
	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max,
									 final Label dflt, final Label... labels) {
		visitSwitchInsn(dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
									  final Label[] labels) {
		visitSwitchInsn(dflt, labels);
	}

	private void visitSwitchInsn(final Label dflt, final Label[] labels) {
		visitInsn();
		LabelInfo.resetDone(labels);
		int branch = 0;
		jumps.add(new Jump(lastInsn, dflt, branch));
		LabelInfo.setDone(dflt);
		for (final Label l : labels) {
			if (!LabelInfo.isDone(l)) {
				branch++;
				jumps.add(new Jump(lastInsn, l, branch));
				LabelInfo.setDone(l);
			}
		}
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		visitInsn();
	}

	@Override
	public void visitProbe(final int probeId) {
		addProbe(probeId, 0);
		lastInsn = null;
	}

	@Override
	public void visitJumpInsnWithProbe(final int opcode, final Label label,
									   final int probeId, final IFrame frame) {
		visitInsn();
		addProbe(probeId, 1);
	}

	@Override
	public void visitInsnWithProbe(final int opcode, final int probeId) {
		visitInsn();
		addProbe(probeId, 0);
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
		visitInsn();
		LabelInfo.resetDone(dflt);
		LabelInfo.resetDone(labels);
		int branch = 0;
		visitSwitchTarget(dflt, branch);
		for (final Label l : labels) {
			branch++;
			visitSwitchTarget(l, branch);
		}
	}

	private void visitSwitchTarget(final Label label, final int branch) {
		final int id = LabelInfo.getProbeId(label);
		if (!LabelInfo.isDone(label)) {
			if (id == LabelInfo.NO_PROBE) {
				jumps.add(new Jump(lastInsn, label, branch));
			} else {
				addProbe(id, branch);
			}
			LabelInfo.setDone(label);
		}
	}

	@Override
	public void visitEnd() {
		// Wire jumps:
		for (final Jump j : jumps) {
			LabelInfo.getInstruction(j.target).setPredecessor(j.source,
					j.branch);
		}

		// ADDED
		// Traces back all instructions that are executed before reaching a probe
		// and stores the mapping from probe to lines in #classCoverageLookup
		for (final CoveredProbe p : coveredProbes) {
			Instruction instruction = p.instruction;
			Set<Integer> coveredLines = new HashSet<>();
			while (instruction != null) {
				coveredLines.add(instruction.getLine());
				instruction = getPredecessor(instruction);
			}
			classCoverageLookup.addProbe(p.probeId, coveredLines);
		}

		// REMOVED the rest of the method
	}

	/**
	 * ADDED
	 * Helper to get the private field predecessor from an instruction.
	 */
	private Instruction getPredecessor(Instruction instruction) {
		try {
			Field f = instruction.getClass().getDeclaredField("predecessor");
			f.setAccessible(true);
			instruction = (Instruction) f.get(instruction);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return instruction;
	}

	private void addProbe(final int probeId, final int branch) {
		lastInsn.addBranch();
		// REMOVED check of probes array and instead add the probes unconditionally
		coveredProbes.add(new CoveredProbe(probeId, lastInsn, branch));
	}

	private static class CoveredProbe {

		// ADDED field probeId to store the probe to instruction mapping
		final int probeId;
		final Instruction instruction;
		final int branch;

		// ADDED probeId
		private CoveredProbe(int probeId, final Instruction instruction, final int branch) {
			this.probeId = probeId;
			this.instruction = instruction;
			this.branch = branch;
		}
	}

	private static class Jump {

		final Instruction source;
		final Label target;
		final int branch;

		Jump(final Instruction source, final Label target, final int branch) {
			this.source = source;
			this.target = target;
			this.branch = branch;
		}
	}

}
