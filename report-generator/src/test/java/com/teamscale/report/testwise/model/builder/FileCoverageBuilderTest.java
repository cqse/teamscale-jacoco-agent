package com.teamscale.report.testwise.model.builder;

import com.teamscale.report.testwise.model.LineRange;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/** Tests the {@link FileCoverageBuilder} class. */
public class FileCoverageBuilderTest {

	/** Tests the compactification algorithm for line ranges. */
	@Test
	public void compactifyRanges() {
		Set<Integer> input = CollectionUtils.asHashSet(1, 3, 4, 6, 7, 10);
		List<LineRange> result = FileCoverageBuilder.compactifyToRanges(input);
		assertEquals("[1, 3-4, 6-7, 10]", result.toString());
	}

	/** Tests the merge of two {@link FileCoverageBuilder} objects. */
	@Test
	public void mergeDoesMergeRanges() {
		FileCoverageBuilder fileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(3, 4);
		fileCoverage.addLineRange(7, 10);

		FileCoverageBuilder otherFileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLineRange(1, 3);
		fileCoverage.addLineRange(12, 14);
		fileCoverage.merge(otherFileCoverage);
		assertEquals("1-4,7-10,12-14", fileCoverage.computeCompactifiedRangesAsString());
	}

	/** Tests that two {@link FileCoverageBuilder} objects from different files throws an exception. */
	@Test(expected = AssertionError.class)
	public void mergeDoesNotAllowMergeOfTwoDifferentFiles() {
		FileCoverageBuilder fileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLine(1);

		FileCoverageBuilder otherFileCoverage = new FileCoverageBuilder("path", "file2");
		fileCoverage.addLineRange(1, 3);
		fileCoverage.merge(otherFileCoverage);
	}

	/** Tests the transformation from line ranges into its string representation. */
	@Test
	public void getRangesAsString() {
		FileCoverageBuilder fileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(3, 4);
		fileCoverage.addLineRange(6, 10);
		assertEquals("1,3-4,6-10", fileCoverage.computeCompactifiedRangesAsString());
	}
}