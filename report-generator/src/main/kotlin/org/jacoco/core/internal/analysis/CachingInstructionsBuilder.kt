package org.jacoco.core.internal.analysis

import com.teamscale.report.testwise.jacoco.cache.ClassCoverageLookup
import com.teamscale.report.util.SortedIntList
import org.jacoco.core.analysis.ISourceNode
import org.jacoco.core.internal.flow.LabelInfo
import org.objectweb.asm.Label
import org.objectweb.asm.tree.AbstractInsnNode
import java.lang.reflect.Field

/**
 * Stateful builder for the [Instruction]s of a method. All instructions of a method must be added in their
 * original sequence along with additional information like line numbers. Afterwards the instructions can be obtained
 * with the `getInstructions()` method.
 *
 *
 * It's core is a copy of [org.jacoco.core.internal.analysis.InstructionsBuilder] that has been extended with
 * caching functionality to speed up report generation.
 *
 *
 * This class contains callbacks for stepping through a method at bytecode level which has been decorated with probes by
 * JaCoCo in a depth-first-search like way.
 *
 *
 * Changes that have been applied to the original class are marked with ADDED and REMOVED comments to make it as easy as
 * possible to adjust the implementation to new versions of JaCoCo.
 *
 *
 * When updating JaCoCo make a diff of the previous [org.jacoco.core.internal.analysis.InstructionsBuilder]
 * implementation and the new implementation and update this class accordingly.
 */
internal class CachingInstructionsBuilder(
	/** Probe array of the class the analyzed method belongs to.  */ // REMOVED private final boolean[] probes;
	// ADDED field to hold a reference to our coverage lookup
	private val classCoverageLookup: ClassCoverageLookup
) : InstructionsBuilder(null) {
	private val coveredProbes: MutableList<CoveredProbe> = ArrayList()

	/** The line which belong to subsequently added instructions.  */
	private var currentLine: Int

	/** The last instruction which has been added.  */
	private var currentInsn: Instruction? = null

	/**
	 * All instructions of a method mapped from the ASM node to the corresponding [Instruction] instance.
	 */
	private val instructions: MutableMap<AbstractInsnNode, Instruction>

	/**
	 * The labels which mark the subsequent instructions.
	 *
	 *
	 * Due to ASM issue #315745 there can be more than one label per instruction
	 */
	private val currentLabel: MutableList<Label>

	/**
	 * List of all jumps within the control flow. We need to store jumps temporarily as the target [Instruction]
	 * may not been known yet.
	 */
	private val jumps: MutableList<Jump>

	/**
	 * Creates a new builder instance which can be used to analyze a single method.
	 *
	 *
	 * ADDED ClassCoverageLookup classCoverageLookup parameter REMOVED final boolean[] probes
	 *
	 * @param classCoverageLookup cache of the class' probes
	 */
	init {
		this.currentLine = ISourceNode.UNKNOWN_LINE
		this.instructions = HashMap()
		this.currentLabel = ArrayList(2)
		this.jumps = ArrayList()
	}

	/**
	 * Sets the current source line. All subsequently added instructions will be assigned to this line. If no line is
	 * set (e.g. for classes compiled without debug information) [ISourceNode.UNKNOWN_LINE] is assigned to the
	 * instructions.
	 */
	override fun setCurrentLine(line: Int) {
		currentLine = line
	}

	/**
	 * Adds a label which applies to the subsequently added instruction. Due to ASM internals multiple [Label]s
	 * can be added to an instruction.
	 */
	override fun addLabel(label: Label) {
		currentLabel.add(label)
		if (!LabelInfo.isSuccessor(label)) {
			noSuccessor()
		}
	}

	/**
	 * Adds a new instruction. Instructions are by default linked with the previous instruction unless specified
	 * otherwise.
	 */
	override fun addInstruction(node: AbstractInsnNode) {
		val insn: Instruction = Instruction(currentLine)
		val labelCount: Int = currentLabel.size
		if (labelCount > 0) {
			var i: Int = labelCount
			while (--i >= 0) {
				LabelInfo.setInstruction(currentLabel.get(i), insn)
			}
			currentLabel.clear()
		}
		if (currentInsn != null) {
			currentInsn!!.addBranch(insn, 0)
		}
		currentInsn = insn
		instructions.put(node, insn)
	}

	/**
	 * Declares that the next instruction will not be a successor of the current instruction. This is the case with an
	 * unconditional jump or technically when a probe was inserted before.
	 */
	override fun noSuccessor() {
		currentInsn = null
	}

	/**
	 * Adds a jump from the last added instruction.
	 *
	 * @param target jump target
	 * @param branch unique branch number
	 */
	override fun addJump(target: Label, branch: Int) {
		jumps.add(Jump(currentInsn, target, branch))
	}

	/**
	 * Adds a new probe for the last instruction.
	 *
	 * @param probeId index in the probe array
	 * @param branch  unique branch number for the last instruction
	 */
	override fun addProbe(probeId: Int, branch: Int) {
		// REMOVED check of probes array and instead add the probes unconditionally
		// final boolean executed = probes != null && probes[probeId];
		// currentInsn.addBranch(executed, branch);

		// ADDED

		currentInsn!!.addBranch(true, branch)
		coveredProbes.add(CoveredProbe(probeId, currentInsn, branch))
	}

	/**
	 * Returns the status for all instructions of this method. This method must be called exactly once after the
	 * instructions have been added.
	 */
	fun fillCache() {
		// Wire jumps:
		for (j: Jump in jumps) {
			j.wire()
		}

		// ADDED
		// Traces back all instructions that are executed before reaching a probe
		// and stores the mapping from probe to lines in #classCoverageLookup
		// We need this because JaCoCo does not insert a probe after every line.
		for (coveredProbe: CoveredProbe in coveredProbes) {
			var instruction: Instruction? = coveredProbe.instruction
			val coveredLines: SortedIntList = SortedIntList()
			while (instruction != null) {
				if (instruction.getLine() != -1) {
					// Only add the line number if one is associated with the instruction.
					// This is not the case for e.g. Lombok generated code.
					coveredLines.add(instruction.getLine())
				}
				instruction = getPredecessor(instruction)
			}
			classCoverageLookup.addProbe(coveredProbe.probeId, coveredLines)
		}
	}

	/**
	 * ADDED Helper to get the private field predecessor from an instruction. The predecessor of an instruction is the
	 * preceding node according to the control flow graph of the method.
	 */
	private fun getPredecessor(instruction: Instruction): Instruction? {
		var instruction: Instruction? = instruction
		try {
			val predecessorField = instruction!!.javaClass.getDeclaredField("predecessor")
			predecessorField.isAccessible = true
			instruction = predecessorField.get(instruction) as? Instruction
		} catch (e: NoSuchFieldException) {
			throw RuntimeException("Instruction has no field named predecessor! This is a programming error!", e)
		} catch (e: IllegalAccessException) {
			throw RuntimeException("Instruction has no field named predecessor! This is a programming error!", e)
		}
		return instruction
	}

	// ADDED
	private class CoveredProbe(val probeId: Int, val instruction: Instruction?, val branch: Int)

	private class Jump(private val source: Instruction?, private val target: Label, private val branch: Int) {
		fun wire() {
			source!!.addBranch(LabelInfo.getInstruction(target), branch)
		}
	}
}
