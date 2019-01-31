package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.FileCoverage
import com.teamscale.report.testwise.model.LineRange
import org.conqat.lib.commons.assertion.CCSMAssert

import java.util.ArrayList
import java.util.Comparator
import java.util.HashSet
import java.util.stream.Collectors
import java.util.stream.IntStream

/** Holds coverage of a single file.  */
class FileCoverageBuilder
/** Constructor.  */
    (
    /** The file system path of the file not including the file itself.  */
    /** @see .path
     */
    val path: String,
    /** The name of the file.  */
    /** @see .fileName
     */
    val fileName: String
) {

    /** A set of line numbers that have been covered.  */
    private val coveredLines = HashSet<Int>()

    /** Returns true if there is no coverage for the file yet.  */
    val isEmpty: Boolean
        get() = coveredLines.isEmpty()

    /** Adds a line as covered.  */
    fun addLine(line: Int) {
        coveredLines.add(line)
    }

    /** Adds a line range as covered.  */
    fun addLineRange(start: Int, end: Int) {
        coveredLines.addAll(start..end)
    }

    /** Adds set of lines as covered.  */
    fun addLines(range: Set<Int>) {
        coveredLines.addAll(range)
    }

    /** Merges the list of ranges into the current list.  */
    fun merge(other: FileCoverageBuilder) {
        CCSMAssert.isTrue(
            other.fileName == fileName && other.path == path,
            "Cannot merge coverage of two different files! This is a bug!"
        )
        coveredLines.addAll(other.coveredLines)
    }

    /**
     * Returns a compact string representation of the covered lines.
     * Continuous line ranges are merged to ranges and sorted.
     * Individual ranges are separated by commas. E.g. 1-5,7,9-11.
     */
    fun computeCompactifiedRangesAsString(): String {
        val coveredRanges = compactifyToRanges(coveredLines)
        return coveredRanges.joinToString(",") { it.toReportString() }
    }

    /** Builds the [FileCoverage] object, which is serialized into the report.  */
    fun build(): FileCoverage {
        return FileCoverage(fileName, computeCompactifiedRangesAsString())
    }

    companion object {

        /**
         * Merges all overlapping and neighboring [LineRange]s.
         * E.g. a list of [[1-5],[3-7],[8-10],[12-14]] becomes [[1-10],[12-14]]
         */
        fun compactifyToRanges(lines: Set<Int>): List<LineRange> {
            if (lines.isEmpty()) {
                return ArrayList()
            }

            val linesList = ArrayList(lines)
            linesList.sortWith(Comparator { obj, anotherInteger -> obj.compareTo(anotherInteger) })

            val firstLine = linesList[0]
            var currentRange = LineRange(firstLine!!, firstLine)

            val compactifiedRanges = ArrayList<LineRange>()
            compactifiedRanges.add(currentRange)

            for (currentLine in linesList) {
                if (currentRange.end == currentLine || currentRange.end == currentLine!! - 1) {
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
