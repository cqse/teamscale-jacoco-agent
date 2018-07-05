package eu.cqse.teamscale.report.testwise.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Tests the {@link FileCoverage} class. */
public class FileCoverageTest {

	/** Tests the compactification algorithm for line ranges. */
	@Test
	public void compactifyRanges() {
		List<LineRange> input = Arrays.asList(
				new LineRange(1, 5),
				new LineRange(3, 7),
				new LineRange(8, 10),
				new LineRange(12, 14)
		);
		List<LineRange> result = FileCoverage.compactifyRanges(input);
		assertEquals("[1-10, 12-14]", result.toString());
	}

	/** Tests the merge of two {@link FileCoverage} objects. */
	@Test
	public void mergeDoesMergeRanges() {
		FileCoverage fileCoverage = new FileCoverage("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(new LineRange(3, 4));
		fileCoverage.addLineRange(7, 10);

		FileCoverage otherFileCoverage = new FileCoverage("path", "file");
		fileCoverage.addLineRange(1, 3);
		fileCoverage.addLineRange(12, 14);
		fileCoverage.merge(otherFileCoverage);
		assertEquals("1-4,7-10,12-14", fileCoverage.computeCompactifiedRangesAsString());
	}

	/** Tests that two {@link FileCoverage} objects from different files throws an exception. */
	@Test(expected = AssertionError.class)
	public void mergeDoesNotAllowMergeOfTwoDifferentFiles() {
		FileCoverage fileCoverage = new FileCoverage("path", "file");
		fileCoverage.addLine(1);

		FileCoverage otherFileCoverage = new FileCoverage("path", "file2");
		fileCoverage.addLineRange(1, 3);
		fileCoverage.merge(otherFileCoverage);
	}

	/** Tests the transformation from line ranges into its string representation. */
	@Test
	public void getRangesAsString() {
		FileCoverage fileCoverage = new FileCoverage("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(new LineRange(3, 4));
		fileCoverage.addLineRange(6, 10);
		assertEquals("1,3-4,6-10", fileCoverage.computeCompactifiedRangesAsString());
	}
}