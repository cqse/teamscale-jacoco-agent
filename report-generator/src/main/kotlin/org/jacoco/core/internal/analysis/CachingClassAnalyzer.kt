package org.jacoco.core.internal.analysis

import com.teamscale.report.testwise.jacoco.cache.ClassCoverageLookup
import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.MethodNode

/**
 * Analyzes a class to reconstruct probe information.
 *
 *
 * A probe lookup holds for a single class which probe belongs to which lines. The actual filling of the
 * [ClassCoverageLookup] happens in [CachingInstructionsBuilder].
 */
class CachingClassAnalyzer
/**
 * Creates a new analyzer that builds coverage data for a class.
 *
 * @param classCoverageLookup cache for the class' probes
 * @param coverage            coverage node for the analyzed class data
 * @param stringPool          shared pool to minimize the number of [String] instances
 */(
	/** The cache, which contains a probe lookups for the current class.  */
	private val classCoverageLookup: ClassCoverageLookup,
	coverage: ClassCoverageImpl?,
	stringPool: StringPool?
) : ClassAnalyzer(coverage, null, stringPool) {
	override fun visitSource(source: String?, debug: String?) {
		super.visitSource(source, debug)
		classCoverageLookup.setSourceFileName(source)
	}

	override fun visitMethod(
		access: Int, name: String?,
		desc: String?, signature: String?, exceptions: Array<String>?
	): MethodProbesVisitor {
		val builder = CachingInstructionsBuilder(classCoverageLookup)

		return object : MethodAnalyzer(builder) {
			override fun accept(
				methodNode: MethodNode,
				methodVisitor: MethodVisitor
			) {
				super.accept(methodNode, methodVisitor)
				builder.fillCache()
			}
		}
	}

	override fun visitTotalProbeCount(count: Int) {
		classCoverageLookup.setTotalProbeCount(count)
	}
}
