package com.teamscale.report.testwise.model.builder;

import com.teamscale.report.testwise.model.FileCoverage;
import com.teamscale.report.testwise.model.LineRange;
import org.conqat.lib.commons.assertion.CCSMAssert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Holds coverage of a single file. */
public class FileCoverageBuilder {

	/** The file system path of the file not including the file itself. */
	private final String path;

	/** The name of the file. */
	private final String fileName;

	/** A set of line numbers that have been covered. */
	private final Set<Integer> coveredLines = new HashSet<>();

	/** Constructor. */
	public FileCoverageBuilder(String path, String file) {
		this.path = path;
		this.fileName = file;
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
		coveredLines.addAll(IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList()));
	}

	/** Adds set of lines as covered. */
	public void addLines(Set<Integer> range) {
		coveredLines.addAll(range);
	}

	/** Merges the list of ranges into the current list. */
	public void merge(FileCoverageBuilder other) {
		CCSMAssert.isTrue(other.fileName.equals(fileName) && other.path.equals(path),
				"Cannot merge coverage of two different files! This is a bug!");
		coveredLines.addAll(other.coveredLines);
	}

	/**
	 * Merges all overlapping and neighboring {@link LineRange}s.
	 * E.g. a list of [[1-5],[3-7],[8-10],[12-14]] becomes [[1-10],[12-14]]
	 */
	public static List<LineRange> compactifyToRanges(Set<Integer> lines) {
		if(lines.isEmpty()) {
			return new ArrayList<>();
		}

		List<Integer> linesList = new ArrayList<>(lines);
		linesList.sort(Integer::compareTo);

		Integer firstLine = linesList.get(0);
		LineRange currentRange = new LineRange(firstLine, firstLine);

		List<LineRange> compactifiedRanges = new ArrayList<>();
		compactifiedRanges.add(currentRange);

		for (Integer currentLine : linesList) {
			if(currentRange.getEnd() == currentLine || currentRange.getEnd() == currentLine - 1) {
				currentRange.setEnd(currentLine);
			} else {
				currentRange = new LineRange(currentLine, currentLine);
				compactifiedRanges.add(currentRange);
			}
		}

		return compactifiedRanges;
	}

	/**
	 * Returns a compact string representation of the covered lines.
	 * Continuous line ranges are merged to ranges and sorted.
	 * Individual ranges are separated by commas. E.g. 1-5,7,9-11.
	 */
	public String computeCompactifiedRangesAsString() {
		List<LineRange> coveredRanges = compactifyToRanges(coveredLines);
		return coveredRanges.stream().map(LineRange::toReportString).collect(Collectors.joining(","));
	}

	/** Returns true if there is no coverage for the file yet. */
	public boolean isEmpty() {
		return coveredLines.isEmpty();
	}

	/** Builds the {@link FileCoverage} object, which is serialized into the report. */
	public FileCoverage build() {
		return new FileCoverage(fileName, computeCompactifiedRangesAsString());
	}
}
