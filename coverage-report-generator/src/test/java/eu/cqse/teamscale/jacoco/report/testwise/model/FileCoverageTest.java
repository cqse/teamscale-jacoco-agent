package eu.cqse.teamscale.jacoco.report.testwise.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FileCoverageTest {

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

	@Test
	public void merge() {
		FileCoverage fileCoverage = new FileCoverage("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(new LineRange(3, 4));
		fileCoverage.addLineRange(7, 10);

		fileCoverage.merge(Arrays.asList(
				new LineRange(1, 3),
				new LineRange(12, 14)
		));
		assertEquals("1-4,7-10,12-14", fileCoverage.getCompactifiedRangesAsString());
	}

	@Test
	public void getRangesAsString() {
		FileCoverage fileCoverage = new FileCoverage("path", "file");
		fileCoverage.addLine(1);
		fileCoverage.addLineRange(new LineRange(3, 4));
		fileCoverage.addLineRange(6, 10);
		assertEquals("1,3-4,6-10", fileCoverage.getCompactifiedRangesAsString());
	}
}