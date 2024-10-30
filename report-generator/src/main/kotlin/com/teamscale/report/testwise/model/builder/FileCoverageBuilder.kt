package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.FileCoverage
import com.teamscale.report.testwise.model.LineRange
import com.teamscale.report.util.SortedIntList
import java.util.stream.Collectors

/** Holds coverage of a single file.  */
class FileCoverageBuilder
/** Constructor.  */(
	/** The file system path of the file not including the file itself.  */
	val path: String,
	/** The name of the file.  */
	val fileName: String
) {
	/**
	 * A list of line numbers that have been covered. Using a set here is too memory intensive.
	 */
	private val coveredLines = SortedIntList()

	/** Adds a line as covered.  */
	fun addLine(line: Int) {
		coveredLines.add(line)
	}

	/** Adds a line range as covered.  */
	fun addLineRange(start: Int, end: Int) {
		for (i in start..end) {
			coveredLines.add(i)
		}
	}

	/** Adds set of lines as covered.  */
	fun addLines(range: SortedIntList) {
		coveredLines.addAll(range)
	}

	/** Merges the list of ranges into the current list.  */
	fun merge(other: FileCoverageBuilder) {
		if (other.fileName != fileName || other.path != path) {
			throw AssertionError("Cannot merge coverage of two different files! This is a bug!")
		}
		coveredLines.addAll(other.coveredLines)
	}

	/**
	 * Returns a compact string representation of the covered lines. Continuous line ranges are merged to ranges and
	 * sorted. Individual ranges are separated by commas. E.g. 1-5,7,9-11.
	 */
	fun computeCompactifiedRangesAsString(): String {
		val coveredRanges = compactifyToRanges(coveredLines)
		return coveredRanges.stream().map { obj: LineRange -> obj.toReportString() }.collect(Collectors.joining(","))
	}

	val isEmpty: Boolean
		/** Returns true if there is no coverage for the file yet.  */
		get() = coveredLines.size() == 0

	/** Builds the [FileCoverage] object, which is serialized into the report.  */
	fun build(): FileCoverage {
		return FileCoverage(fileName, computeCompactifiedRangesAsString())
	}

	companion object {
		/**
		 * Merges all neighboring line numbers to ranges. E.g. a list of [[1-5],[3-7],[8-10],[12-14]] becomes
		 * [[1-10],[12-14]]
		 */
		@JvmStatic
		fun compactifyToRanges(lines: SortedIntList): List<LineRange> {
			if (lines.size() == 0) {
				return ArrayList()
			}

			val firstLine = lines[0]
			var currentRange = LineRange(firstLine, firstLine)

			val compactifiedRanges: MutableList<LineRange> = ArrayList()
			compactifiedRanges.add(currentRange)

			for (i in 0 until lines.size()) {
				val currentLine = lines[i]
				if (currentRange.end == currentLine || currentRange.end == currentLine - 1) {
					currentRange.end = currentLine
				} else {
					currentRange = LineRange(currentLine, currentLine)
					compactifiedRanges.add(currentRange)
				}
			}
			return compactifiedRanges
		}
	}
}
