package com.teamscale.report.testwise.model.builder;

import com.teamscale.report.testwise.model.FileCoverage;
import com.teamscale.report.testwise.model.LineRange;
import com.teamscale.report.util.SortedIntList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Holds coverage of a single file. */
public class FileCoverageBuilder {

	/** The file system path of the file not including the file itself. */
	private final String path;

	/** The name of the file. */
	private final String fileName;

	/**
	 * A list of line numbers that have been covered. Using a set here is too memory intensive.
	 */
	private final SortedIntList coveredLines = new SortedIntList();

	/** Constructor. */
	public FileCoverageBuilder(String path, String fileName) {
		this.path = path;
		this.fileName = fileName;
	}

	/** @see #fileName */
	public String getFileName() {
		return fileName;
	}

	/** @see #path */
	public String getPath() {
		return path;
	}

	/** Adds a line as covered. */
	public void addLine(int line) {
		coveredLines.add(line);
	}

	/** Adds a line range as covered. */
	public void addLineRange(int start, int end) {
		for (int i = start; i <= end; i++) {
			coveredLines.add(i);
		}
	}

	/** Adds set of lines as covered. */
	public void addLines(SortedIntList range) {
		coveredLines.addAll(range);
	}

	/** Merges the list of ranges into the current list. */
	public void merge(FileCoverageBuilder other) {
		if (!other.fileName.equals(fileName) || !other.path.equals(path)) {
			throw new AssertionError("Cannot merge coverage of two different files! This is a bug!");
		}
		coveredLines.addAll(other.coveredLines);
	}

	/**
	 * Merges all neighboring line numbers to ranges. E.g. a list of [[1-5],[3-7],[8-10],[12-14]] becomes
	 * [[1-10],[12-14]]
	 */
	public static List<LineRange> compactifyToRanges(SortedIntList lines) {
		if (lines.size() == 0) {
			return new ArrayList<>();
		}

		int firstLine = lines.get(0);
		LineRange currentRange = new LineRange(firstLine, firstLine);

		List<LineRange> compactifiedRanges = new ArrayList<>();
		compactifiedRanges.add(currentRange);

		for (int i = 0; i < lines.size(); i++) {
			int currentLine = lines.get(i);
			if (currentRange.getEnd() == currentLine || currentRange.getEnd() == currentLine - 1) {
				currentRange.setEnd(currentLine);
			} else {
				currentRange = new LineRange(currentLine, currentLine);
				compactifiedRanges.add(currentRange);
			}
		}
		return compactifiedRanges;
	}

	/**
	 * Returns a compact string representation of the covered lines. Continuous line ranges are merged to ranges and
	 * sorted. Individual ranges are separated by commas. E.g. 1-5,7,9-11.
	 */
	public String computeCompactifiedRangesAsString() {
		List<LineRange> coveredRanges = compactifyToRanges(coveredLines);
		return coveredRanges.stream().map(LineRange::toReportString).collect(Collectors.joining(","));
	}

	/** Returns true if there is no coverage for the file yet. */
	public boolean isEmpty() {
		return coveredLines.size() == 0;
	}

	/** Builds the {@link FileCoverage} object, which is serialized into the report. */
	public FileCoverage build() {
		return new FileCoverage(fileName, computeCompactifiedRangesAsString());
	}
}
