package com.teamscale.report.testwise.model.builder;

import com.teamscale.report.testwise.model.LineRange;
import com.teamscale.report.util.SortedIntList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the {@link FileCoverageBuilder} class. */
class FileCoverageBuilderTest {

	/** Tests the compactification algorithm for line ranges. */
	@Test
	void compactifyRanges() {
		SortedIntList sortedIntList = new SortedIntList();
		sortedIntList.add(1);
		sortedIntList.add(3);
		sortedIntList.add(4);
		sortedIntList.add(6);
		sortedIntList.add(7);
		sortedIntList.add(10);
		List<LineRange> result = FileCoverageBuilder.compactifyToRanges(sortedIntList);
		assertThat(result).hasToString("[1, 3-4, 6-7, 10]");
	}

	/** Tests the merge of two {@link FileCoverageBuilder} objects. */
	@Test
	void mergeDoesMergeRanges() {
		FileCoverageBuilder fileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(3, 4);
		fileCoverage.addLineRange(7, 10);

		FileCoverageBuilder otherFileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLineRange(1, 3);
		fileCoverage.addLineRange(12, 14);
		fileCoverage.merge(otherFileCoverage);
		assertThat(fileCoverage.computeCompactifiedRangesAsString()).isEqualTo("1-4,7-10,12-14");
	}

	/** Tests that two {@link FileCoverageBuilder} objects from different files throws an exception. */
	@Test
	void mergeDoesNotAllowMergeOfTwoDifferentFiles() {
		FileCoverageBuilder fileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLine(1);

		FileCoverageBuilder otherFileCoverage = new FileCoverageBuilder("path", "file2");
		fileCoverage.addLineRange(1, 3);
		assertThatCode(() -> fileCoverage.merge(otherFileCoverage)).isInstanceOf(AssertionError.class);
	}

	/** Tests the transformation from line ranges into its string representation. */
	@Test
	void getRangesAsString() {
		FileCoverageBuilder fileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(3, 4);
		fileCoverage.addLineRange(6, 10);
		assertEquals("1,3-4,6-10", fileCoverage.computeCompactifiedRangesAsString());
	}
}