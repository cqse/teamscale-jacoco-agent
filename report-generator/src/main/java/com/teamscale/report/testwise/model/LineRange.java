package com.teamscale.report.testwise.model;

/** Holds a line range with start and end (both inclusive and 1-based). */
public class LineRange {

	/** Indicates that no specific line has been set yet. */
	private static final int UNKNOWN_LINE = -1;

	/** The start line (1-based). The initial value is {@link #UNKNOWN_LINE} if not set via the constructor. */
	private int start;

	/** The end line (1-based). The initial value is {@link #UNKNOWN_LINE} if not set via the constructor. */
	private int end;

	/** Constructs a line range with start and end set to {@link #UNKNOWN_LINE}. */
	public LineRange() {
		start = UNKNOWN_LINE;
		end = UNKNOWN_LINE;
	}

	/** Constructor. */
	public LineRange(int start, int end) {
		this.start = start;
		this.end = end;
	}

	/** Adjusts either the start or end of the range to include the given line afterwards. */
	public void adjustToContain(int line) {
		if (start > line || start == UNKNOWN_LINE) {
			start = line;
		}
		if (end < line || end == UNKNOWN_LINE) {
			end = line;
		}
	}

	/** @see #start */
	public int getStart() {
		return start;
	}

	/** @see #end */
	public int getEnd() {
		return end;
	}

	/** Checks if the line region does contain any lines. */
	public boolean isEmpty() {
		return start == UNKNOWN_LINE && end == UNKNOWN_LINE;
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
