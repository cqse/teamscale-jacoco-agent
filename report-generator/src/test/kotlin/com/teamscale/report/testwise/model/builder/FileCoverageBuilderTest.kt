package com.teamscale.report.testwise.model.builder

import com.teamscale.report.testwise.model.builder.FileCoverageBuilder.Companion.compactifyToRanges
import com.teamscale.report.util.CompactLines.Companion.compactLinesOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Tests the [FileCoverageBuilder] class.  */
internal class FileCoverageBuilderTest {
	/** Tests the compactification algorithm for line ranges.  */
	@Test
	fun compactifyRanges() {
		val compactLines = compactLinesOf(1, 3, 4, 6, 7, 10)
		assertThat(compactifyToRanges(compactLines))
			.hasToString("[1, 3-4, 6-7, 10]")
	}

	/** Tests the merge of two [FileCoverageBuilder] objects.  */
	@Test
	fun mergeDoesMergeRanges() {
		FileCoverageBuilder("path", "file").apply {
			addLine(1)
			addLineRange(3, 4)
			addLineRange(7, 10)

			addLineRange(1, 3)
			addLineRange(12, 14)
			val otherFileCoverage = FileCoverageBuilder("path", "file")
			merge(otherFileCoverage)
			assertThat(computeCompactifiedRangesAsString()).isEqualTo("1-4,7-10,12-14")
		}
	}

	/** Tests that two [FileCoverageBuilder] objects from different files throws an exception.  */
	@Test
	fun mergeDoesNotAllowMergeOfTwoDifferentFiles() {
		FileCoverageBuilder("path", "file").apply {
			addLine(1)
			addLineRange(1, 3)
			val otherFileCoverage = FileCoverageBuilder("path", "file2")
			assertThatCode {
				merge(otherFileCoverage)
			}.isInstanceOf(IllegalArgumentException::class.java)
		}
	}

	@Test
	/** Tests the transformation from line ranges into its string representation.  */
	fun getRangesAsString() {
		FileCoverageBuilder("path", "file").apply {
			addLine(1)
			addLineRange(3, 4)
			addLineRange(6, 10)
			assertEquals(
				"1,3-4,6-10",
				computeCompactifiedRangesAsString()
			)
		}
	}
}