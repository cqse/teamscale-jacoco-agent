package com.teamscale.report.testwise.model;

/** Holds a line range with start and end (both inclusive and 1-based). */
public class LineRange {

	/** The start line (1-based). */
	private int start;

	/** The end line (1-based). */
	private int end;

	/** Constructor. */
	public LineRange(int start, int end) {
		this.start = start;
		this.end = end;
	}

	/** @see #start */
	public int getStart() {
		return start;
	}

	/** @see #end */
	public int getEnd() {
		return end;
	}

	/** @see #end */
	public void setEnd(int end) {
		this.end = end;
	}

	/**
	 * Returns the line range as used in the XML report.
	 * A range is returned as e.g. 2-5 or simply 3 if the start and end are equal.
	 */
	public String toReportString() {
		if (start == end) {
			return String.valueOf(start);
		} else {
			return start + "-" + end;
		}
	}

	@Override
	public String toString() {
		return toReportString();
	}
}
