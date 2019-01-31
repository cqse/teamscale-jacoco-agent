package com.teamscale.report.testwise.model.builder

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests the [FileCoverageBuilder] class.  */
class FileCoverageBuilderTest {

    /** Tests the compactification algorithm for line ranges.  */
    @Test
    fun compactifyRanges() {
        val input = setOf(1, 3, 4, 6, 7, 10)
        val result = FileCoverageBuilder.compactifyToRanges(input)
        assertEquals("[1, 3-4, 6-7, 10]", result.toString())
    }

    /** Tests the merge of two [FileCoverageBuilder] objects.  */
    @Test
    fun mergeDoesMergeRanges() {
        val fileCoverage = FileCoverageBuilder("path", "file")
        fileCoverage.addLine(1)
        fileCoverage.addLineRange(3, 4)
        fileCoverage.addLineRange(7, 10)

        val otherFileCoverage = FileCoverageBuilder("path", "file")
        fileCoverage.addLineRange(1, 3)
        fileCoverage.addLineRange(12, 14)
        fileCoverage.merge(otherFileCoverage)
        assertEquals("1-4,7-10,12-14", fileCoverage.computeCompactifiedRangesAsString())
    }

    /** Tests that two [FileCoverageBuilder] objects from different files throws an exception.  */
    @Test(expected = IllegalArgumentException::class)
    fun mergeDoesNotAllowMergeOfTwoDifferentFiles() {
        val fileCoverage = FileCoverageBuilder("path", "file")
        fileCoverage.addLine(1)

        val otherFileCoverage = FileCoverageBuilder("path", "file2")
        fileCoverage.addLineRange(1, 3)
        fileCoverage.merge(otherFileCoverage)
    }

    /** Tests the transformation from line ranges into its string representation.  */
    @Test
    fun getRangesAsString() {
        val fileCoverage = FileCoverageBuilder("path", "file")
        fileCoverage.addLine(1)
        fileCoverage.addLineRange(3, 4)
        fileCoverage.addLineRange(6, 10)
        assertEquals("1,3-4,6-10", fileCoverage.computeCompactifiedRangesAsString())
    }
}