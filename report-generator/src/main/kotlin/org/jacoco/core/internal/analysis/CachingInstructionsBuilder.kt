package org.jacoco.core.internal.analysis

import com.teamscale.report.testwise.jacoco.cache.ClassCoverageLookup
import org.jacoco.core.analysis.ISourceNode
import org.jacoco.core.internal.flow.LabelInfo
import org.objectweb.asm.Label
import org.objectweb.asm.tree.AbstractInsnNode

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
 *
 * @param classCoverageLookup probe array of the class the analyzed method belongs to.
 */
internal class CachingInstructionsBuilder(
	private val classCoverageLookup: ClassCoverageLookup
) : InstructionsBuilder(null) {
	private val coveredProbes = mutableListOf<CoveredProbe>()

	/** The line which belong to subsequently added instructions.  */
	private var currentLine: Int = ISourceNode.UNKNOWN_LINE

	/** The last instruction which has been added.  */
	private var currentInstruction: Instruction? = null

	/**
	 * All instructions of a method mapped from the ASM node to the corresponding [Instruction] instance.
	 */
	private val instructions = mutableMapOf<AbstractInsnNode, Instruction>()

	/**
	 * The labels which mark the subsequent instructions.
	 *
	 *
	 * Due to ASM issue #315745 there can be more than one label per instruction
	 */
	private val currentLabel = mutableListOf<Label>()

	/**
	 * List of all jumps within the control flow. We need to store jumps temporarily as the target [Instruction]
	 * may not been known yet.
	 */
	private val jumps = mutableListOf<Jump>()

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
		if (!LabelInfo.isSuccessor(label)) noSuccessor()
	}

	/**
	 * Adds a new instruction. Instructions are by default linked with the previous instruction unless specified
	 * otherwise.
	 */
	override fun addInstruction(node: AbstractInsnNode) {
		Instruction(currentLine).also {
			currentLabel.forEach { label -> LabelInfo.setInstruction(label, it) }
			currentLabel.clear()
			currentInstruction?.addBranch(it, 0)
			currentInstruction = it
			instructions[node] = it
		}
	}

	/**
	 * Declares that the next instruction will not be a successor of the current instruction. This is the case with an
	 * unconditional jump or technically when a probe was inserted before.
	 */
	override fun noSuccessor() {
		currentInstruction = null
	}

	/**
	 * Adds a jump from the last added instruction.
	 *
	 * @param target jump target
	 * @param branch unique branch number
	 */
	override fun addJump(target: Label, branch: Int) {
		jumps.add(Jump(currentInstruction, target, branch))
	}

	/**
	 * Adds a new probe for the last instruction.
	 *
	 * @param probeId index in the probe array
	 * @param branch  unique branch number for the last instruction
	 */
	override fun addProbe(probeId: Int, branch: Int) {
		currentInstruction?.addBranch(true, branch)
		coveredProbes.add(CoveredProbe(probeId, currentInstruction, branch))
	}

	/**
	 * Returns the status for all instructions of this method. This method must be called exactly once after the
	 * instructions have been added.
	 */
	fun fillCache() {
		jumps.forEach { it.wire() }
		coveredProbes.forEach { coveredProbe ->
			traceInstructionsToProbe(coveredProbe).let { lines ->
				classCoverageLookup.addProbe(coveredProbe.probeId, lines)
			}
		}
	}

	private fun traceInstructionsToProbe(coveredProbe: CoveredProbe) =
		generateSequence(coveredProbe.instruction) { getPredecessor(it) }
			.mapNotNull { it.line.takeIf { line -> line != ISourceNode.UNKNOWN_LINE } }
			.toSortedSet()

	/**
	 * The predecessor of an instruction is the preceding node, according to the control flow graph of the method.
	 */
	private fun getPredecessor(instruction: Instruction?) =
		instruction?.let {
			runCatching {
				it.javaClass.getDeclaredField("predecessor").apply { isAccessible = true }.get(it) as? Instruction
			}.getOrElse { e ->
				throw RuntimeException("Unable to access predecessor field. This is a programming error!", e)
			}
		}

	private class CoveredProbe(val probeId: Int, val instruction: Instruction?, val branch: Int)

	private class Jump(private val source: Instruction?, private val target: Label, private val branch: Int) {
		fun wire() {
			source?.addBranch(LabelInfo.getInstruction(target), branch)
		}
	}
}
