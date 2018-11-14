package eu.cqse.teamscale.report.testwise.model;

import eu.cqse.teamscale.report.testwise.model.builder.FileCoverageBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Tests the {@link FileCoverageBuilder} class. */
public class FileCoverageBuilderTest {

	/** Tests the compactification algorithm for line ranges. */
	@Test
	public void compactifyRanges() {
		List<LineRange> input = Arrays.asList(
				new LineRange(1, 5),
				new LineRange(3, 7),
				new LineRange(8, 10),
				new LineRange(12, 14)
		);
		List<LineRange> result = FileCoverageBuilder.compactifyRanges(input);
		assertEquals("[1-10, 12-14]", result.toString());
	}

	/** Tests the merge of two {@link FileCoverageBuilder} objects. */
	@Test
	public void mergeDoesMergeRanges() {
		FileCoverageBuilder fileCoverage = new FileCoverageBuilder("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(new LineRange(3, 4));
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
		fileCoverage.addLineRange(new LineRange(3, 4));
		fileCoverage.addLineRange(6, 10);
		assertEquals("1,3-4,6-10", fileCoverage.computeCompactifiedRangesAsString());
	}
}