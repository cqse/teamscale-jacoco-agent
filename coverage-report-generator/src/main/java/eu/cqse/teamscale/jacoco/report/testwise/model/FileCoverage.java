package eu.cqse.teamscale.jacoco.report.testwise.model;

import org.conqat.lib.commons.assertion.CCSMAssert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

/** Holds coverage of a single file. */
public class FileCoverage {

	/** The file system path of the file not including the file itself. */
	@XmlTransient
	public String path;

	/** The name of the file. */
	@XmlAttribute(name = "name")
	public String fileName;

	/** A list of line ranges that have been covered. */
	private List<LineRange> coveredRanges = new ArrayList<>();

	/** Constructor. */
	public FileCoverage(String path, String file) {
		this.path = path;
		this.fileName = file;
	}

	/** Adds a line as covered. */
	public void addLine(int line) {
		coveredRanges.add(new LineRange(line, line));
	}

	/** Adds a line range as covered. */
	public void addLineRange(int start, int end) {
		coveredRanges.add(new LineRange(start, end));
	}

	/** Adds a line range as covered. */
	public void addLineRange(LineRange range) {
		coveredRanges.add(range);
	}

	/** Merges the list of ranges into the current list. */
	public void merge(FileCoverage other) {
		CCSMAssert.isTrue(other.fileName.equals(fileName) && other.path.equals(path),
				"Cannot merge coverage of two different files! This is a bug!");
		coveredRanges.addAll(other.coveredRanges);
	}

	/**
	 * Merges all overlapping and neighboring {@link LineRange}s.
	 * E.g. a list of [[1-5],[3-7],[8-10],[12-14]] gets [[1-10],[12-14]]
	 */
	public static List<LineRange> compactifyRanges(List<LineRange> intervals) {
		if (intervals.size() < 2)
			return intervals;

		Collections.sort(intervals);

		LineRange first = intervals.get(0);
		int start = first.getStart();
		int end = first.getEnd();

		ArrayList<LineRange> compactifiedRanges = new ArrayList<>();

		for (int i = 1; i < intervals.size(); i++) {
			LineRange current = intervals.get(i);
			if (current.getStart() <= end + 1) {
				end = Math.max(current.getEnd(), end);
			} else {
				compactifiedRanges.add(new LineRange(start, end));
				start = current.getStart();
				end = current.getEnd();
			}
		}

		compactifiedRanges.add(new LineRange(start, end));

		return compactifiedRanges;
	}

	/**
	 * Returns a compact string representation of the covered line ranges.
	 * Overlapping and directly neighboring ranges are merged and ranges sorted by start line.
	 * Individual ranges are separated by commas. E.g. 1-5,7,9-11
	 */
	public String getCompactifiedRangesAsString() {
		coveredRanges = compactifyRanges(coveredRanges);
		StringBuilder nrString = new StringBuilder();
		for (LineRange lineRange : coveredRanges) {
			if (nrString.length() > 0) {
				nrString.append(",");
			}
			nrString.append(lineRange.toString());
		}
		return nrString.toString();
	}

	/** Returns a lines element used for XML serialization containing all ranges in the compactified format. */
	@XmlElement
	public LinesElement getLines() {
		return new LinesElement(getCompactifiedRangesAsString());
	}

	/** Returns true if there is no coverage for the file yet. */
	public boolean isEmpty() {
		return coveredRanges.isEmpty();
	}

	/** Container for the "lines" xml tag. */
	public static class LinesElement {

		/** The string representation of the covered line ranges. */
		@XmlAttribute(name = "nr")
		public final String lineRangesAsString;

		/** Constructor. */
		public LinesElement(String lineRangesAsString) {
			this.lineRangesAsString = lineRangesAsString;
		}
	}
}
