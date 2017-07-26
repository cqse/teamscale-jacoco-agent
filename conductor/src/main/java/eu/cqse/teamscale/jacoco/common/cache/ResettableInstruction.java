package eu.cqse.teamscale.jacoco.common.cache;

import org.jacoco.core.internal.flow.Instruction;

public class ResettableInstruction extends Instruction {

    private int coveredBranches;

    private ResettableInstruction predecessor;

    /**
     * New instruction at the given line.
     *
     * @param line source line this instruction belongs to
     */
    public ResettableInstruction(int line) {
        super(line);
        this.coveredBranches = 0;
    }

    /**
     * Sets the given instruction as a predecessor of this instruction. This
     * will add an branch to the predecessor.
     *
     * @see #addBranch()
     * @param predecessor
     *            predecessor instruction
     */
    @Override
    public void setPredecessor(final Instruction predecessor) {
        this.predecessor = (ResettableInstruction) predecessor;
        predecessor.addBranch();
    }

    /**
     * Marks one branch of this instruction as covered. Also recursively marks
     * all predecessor instructions as covered if this is the first covered
     * branch.
     */
    @Override
    public void setCovered() {
        ResettableInstruction i = this;
        while (i != null && i.coveredBranches++ == 0) {
            i = i.predecessor;
        }
    }

    /**
     * Returns the number of covered branches starting from this instruction.
     *
     * @return number of covered branches
     */
    @Override
    public int getCoveredBranches() {
        return coveredBranches;
    }

    void resetCoveredBranches() {
        coveredBranches = 0;
    }
}
