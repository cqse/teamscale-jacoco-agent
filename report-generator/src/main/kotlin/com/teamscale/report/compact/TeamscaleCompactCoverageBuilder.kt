package com.teamscale.report.compact

import org.jacoco.core.analysis.IClassCoverage
import org.jacoco.core.analysis.ICounter
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.analysis.ISourceNode
import java.util.*

/**
 * TODO [ICoverageVisitor] that can ignore non-identical duplicate classes or classes without coverage. In addition,
 * coverage returned via [.getBundle] will only return source file coverage because Teamscale does not
 * need class coverage anyway. This reduces XML size by approximately half.
 */
class TeamscaleCompactCoverageBuilder : ICoverageVisitor {

	private val coverageBySourceFile: SortedMap<String, TeamscaleCompactCoverageReport.CompactCoverageFileInfo> =
		sortedMapOf()

	/** {@inheritDoc} */
	override fun visitCoverage(coverage: IClassCoverage) {
		val sourceFileName = coverage.sourceFileName
		val key = if (coverage.packageName.isNotEmpty()) {
			"${coverage.packageName}/$sourceFileName"
		} else {
			sourceFileName
		}
		val sourceFile = coverageBySourceFile.computeIfAbsent(key) {
			TeamscaleCompactCoverageReport.CompactCoverageFileInfo(it)
		}
		sourceFile.addAll(coverage)
	}

	/**
	 * Increments all counters by the values of the given child. When
	 * incrementing the line counter it is assumed that the child refers to the
	 * same source file.
	 *
	 * @param child
	 * child node to add
	 */
	private fun TeamscaleCompactCoverageReport.CompactCoverageFileInfo.addAll(child: ISourceNode) {
		if (child.firstLine == ISourceNode.UNKNOWN_LINE || child.lastLine == ISourceNode.UNKNOWN_LINE) {
			return
		}
		for (lineNumber in child.firstLine..child.lastLine) {
			val line = child.getLine(lineNumber)
			if (line.status == ICounter.FULLY_COVERED) {
				fullyCoveredLines.add(lineNumber)
				partiallyCoveredLines!!.remove(lineNumber)
			} else if (line.status == ICounter.PARTLY_COVERED) {
				partiallyCoveredLines!!.add(lineNumber)
			}
		}
	}

	/** Creates a compact coverage report from the collected data. */
	fun buildReport(): TeamscaleCompactCoverageReport {
		return TeamscaleCompactCoverageReport(1, coverageBySourceFile.values.toList())
	}
}
