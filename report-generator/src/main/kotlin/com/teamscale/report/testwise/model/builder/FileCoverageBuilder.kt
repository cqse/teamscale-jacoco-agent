package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.FileCoverage
import com.teamscale.report.testwise.model.LineRange
import java.util.SortedSet
import java.util.TreeSet

/** Holds coverage of a single file.  */
class FileCoverageBuilder(
	/** The file system path of the file not including the file itself.  */
	val path: String,
	/** The name of the file.  */
	val fileName: String
) {
	/**
	 * A set of line numbers that have been covered. Ensures order and uniqueness.
	 */
	private val coveredLines = sortedSetOf<Int>()

	/** Adds a line as covered.  */
	fun addLine(line: Int) = coveredLines.add(line)

	/** Adds a line range as covered.  */
	fun addLineRange(start: Int, end: Int) = (start..end).forEach { coveredLines.add(it) }

	/** Adds a set of lines as covered.  */
	fun addLines(lines: Set<Int>) = coveredLines.addAll(lines)

	/** Merges the coverage of another [FileCoverageBuilder] into the current list.  */
	fun merge(other: FileCoverageBuilder) {
		require(other.fileName == fileName && other.path == path) {
			"Cannot merge coverage of two different files! This is a bug!"
		}
		coveredLines.addAll(other.coveredLines)
	}

	/**
	 * Returns a compact string representation of the covered lines. Continuous line ranges are merged to ranges and
	 * sorted. Individual ranges are separated by commas. E.g. 1-5,7,9-11.
	 */
	fun computeCompactifiedRangesAsString(): String =
		compactifyToRanges(coveredLines).joinToString(",")

	/** Returns true if there is no coverage for the file yet.  */
	val isEmpty: Boolean get() = coveredLines.isEmpty()

	/** Builds the [FileCoverage] object, which is serialized into the report.  */
	fun build(): FileCoverage = FileCoverage(fileName, computeCompactifiedRangesAsString())

	companion object {
		/**
		 * Merges all neighboring line numbers to ranges. E.g. a list of [[1-5],[3-7],[8-10],[12-14]] becomes
		 * [[1-10],[12-14]]
		 */
		@JvmStatic
		fun compactifyToRanges(lines: SortedSet<Int>): List<LineRange> =
			lines.fold(mutableListOf()) { ranges, line ->
				if (ranges.isNotEmpty() && ranges.last().end >= line - 1) {
					ranges.last().end = line
				} else {
					ranges.add(LineRange(line, line))
				}
				ranges
			}
	}
}
