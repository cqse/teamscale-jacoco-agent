/*******************************************************************************
 * Copyright (c) 2009, 2017 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package eu.cqse.teamscale.jacoco.current.analysis;

import eu.cqse.teamscale.jacoco.common.cache.ProbeLookup;
import eu.cqse.teamscale.jacoco.common.cache.ResettableInstruction;
import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.core.internal.flow.IFrame;
import org.jacoco.core.internal.flow.Instruction;
import org.jacoco.core.internal.flow.LabelInfo;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link MethodProbesVisitor} that analyzes which statements and branches of
 * a method have been executed based on given probe data.
 * <p>
 * It's core is a copy of {@link org.jacoco.core.internal.analysis.MethodAnalyzer} that has been
 * extended with caching functionality to speed up report generation.
 */
public class CachingMethodAnalyzer extends MethodProbesVisitor {

    private final ProbeLookup probeLookup;

    private int currentLine = ISourceNode.UNKNOWN_LINE;

    // Due to ASM issue #315745 there can be more than one label per instruction
    private final List<Label> currentLabel = new ArrayList<>(2);

    /**
     * List of all jumps encountered
     */
    private final List<Jump> jumps = new ArrayList<>();

    /**
     * Last instruction in byte code sequence
     */
    private ResettableInstruction lastInsn;

    /**
     * New Method analyzer for the given probe data.
     *
     * @param probeLookup cache of the class' probes
     */
    public CachingMethodAnalyzer(ProbeLookup probeLookup) {
        super();
        this.probeLookup = probeLookup;
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
    }

    private void visitInsn() {
        final ResettableInstruction insn = new ResettableInstruction(currentLine);
        probeLookup.addInstruction(insn);
        if (lastInsn != null) {
            insn.setPredecessor(lastInsn);
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
        jumps.add(new Jump(lastInsn, label));
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
        jumps.add(new Jump(lastInsn, dflt));
        LabelInfo.setDone(dflt);
        for (final Label l : labels) {
            if (!LabelInfo.isDone(l)) {
                jumps.add(new Jump(lastInsn, l));
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
        addProbe(probeId);
        lastInsn = null;
    }

    @Override
    public void visitJumpInsnWithProbe(final int opcode, final Label label,
                                       final int probeId, final IFrame frame) {
        visitInsn();
        addProbe(probeId);
    }

    @Override
    public void visitInsnWithProbe(final int opcode, final int probeId) {
        visitInsn();
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
        visitInsn();
        LabelInfo.resetDone(dflt);
        LabelInfo.resetDone(labels);
        visitSwitchTarget(dflt);
        for (final Label l : labels) {
            visitSwitchTarget(l);
        }
    }

    private void visitSwitchTarget(final Label label) {
        final int id = LabelInfo.getProbeId(label);
        if (!LabelInfo.isDone(label)) {
            if (id == LabelInfo.NO_PROBE) {
                jumps.add(new Jump(lastInsn, label));
            } else {
                addProbe(id);
            }
            LabelInfo.setDone(label);
        }
    }

    @Override
    public void visitEnd() {
        // Wire jumps:
        for (final Jump j : jumps) {
            LabelInfo.getInstruction(j.target).setPredecessor(j.source);
        }
    }

    private void addProbe(final int probeId) {
        lastInsn.addBranch();
        probeLookup.addProbe(probeId, lastInsn);
    }

    private static class Jump {

        final Instruction source;
        final Label target;

        Jump(final Instruction source, final Label target) {
            this.source = source;
            this.target = target;
        }
    }

}
