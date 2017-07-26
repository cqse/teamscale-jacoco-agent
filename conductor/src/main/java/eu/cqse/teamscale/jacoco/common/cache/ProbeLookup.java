package eu.cqse.teamscale.jacoco.common.cache;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.jacoco.core.internal.analysis.CounterImpl;
import org.jacoco.core.internal.flow.Instruction;
import org.jacoco.core.internal.data.CRC64;

import java.util.ArrayList;
import java.util.List;

public class ProbeLookup {

    /**
     * The class id used in the execution data ({@link CRC64} checksum of the class file).
     */
    private final long classId;

    /**
     * Fully qualified name of the class.
     */
    private String className;

    /**
     * Name of the java source file
     */
    private String sourceFileName;

    /**
     * List of all instructions of this class.
     */
    private List<ResettableInstruction> instructions = new ArrayList<>();

    /**
     * List of all instructions that have a probe attached to them.
     * This is subset of {@link #instructions}. If this probe has been
     * executed all predecessors ({@link ResettableInstruction#predecessor})
     * have been as well.
     */
    private List<ResettableInstruction> probes = new ArrayList<>();

    private int firstLine = ISourceNode.UNKNOWN_LINE;
    private int lastLine = ISourceNode.UNKNOWN_LINE;

    public ProbeLookup(long classId, String className) {
        this.classId = classId;
        this.className = className;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public void addProbe(int probeId, ResettableInstruction instruction) {
        ensureArraySize(probeId);
        probes.set(probeId, instruction);
    }

    public void addInstruction(ResettableInstruction instruction) {
        instructions.add(instruction);
        visitLine(instruction.getLine());
    }

    private void visitLine(int line) {
        if (firstLine > line || lastLine == ISourceNode.UNKNOWN_LINE) {
            firstLine = line;
        }
        if (lastLine < line) {
            lastLine = line;
        }
    }

    private void ensureArraySize(int index) {
        while (index >= probes.size()) {
            probes.add(null);
        }
    }

    private void resetInstructions() {
        for (ResettableInstruction instruction : instructions) {
            if (instruction != null) {
                instruction.resetCoveredBranches();
            }
        }
    }

    public ClassCoverageImpl getClassCoverage(ExecutionData executionData) {
        boolean[] executedProbes = executionData.getProbes();

        // Check probe invariant
        if (probes.size() > executedProbes.length) {
            throw new RuntimeException("Probe lookup does not match with actual probe size for " +
                    sourceFileName + " " + className + " (" + probes.size() + " vs " + executedProbes.length + ")!");
        }

        // Reconstruct branch coverage
        resetInstructions();
        for (int i = 0; i < probes.size(); i++) {
            if (executedProbes[i] && probes.get(i) != null) {
                probes.get(i).setCovered();
            }
        }

        // Build class coverage object
        final ClassCoverageImpl coverage = new ClassCoverageImpl(className, classId, false);
        coverage.setSourceFileName(sourceFileName == null ? className : sourceFileName);
        coverage.ensureCapacity(firstLine, lastLine);
        for (final Instruction instruction : instructions) {
            final int total = instruction.getBranches();
            final int covered = instruction.getCoveredBranches();
            final ICounter instrCounter = covered == 0 ? CounterImpl.COUNTER_1_0
                    : CounterImpl.COUNTER_0_1;
            final ICounter branchCounter = total > 1 ? CounterImpl.getInstance(
                    total - covered, covered) : CounterImpl.COUNTER_0_0;
            coverage.increment(instrCounter, branchCounter, instruction.getLine());
        }
        return coverage;
    }
}
