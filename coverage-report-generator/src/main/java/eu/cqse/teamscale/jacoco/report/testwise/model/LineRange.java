package eu.cqse.teamscale.jacoco.report.testwise.model;

import org.jacoco.core.analysis.ISourceNode;

/** Holds a line range with start and end (both inclusive and 1-based). */
public class LineRange implements Comparable<LineRange> {

	/** The start line (1-based) */
	private int start = ISourceNode.UNKNOWN_LINE;

	/** The end line (1-based) */
	private int end = ISourceNode.UNKNOWN_LINE;

	/** Constructor. */
	public LineRange() {
	}

	/** Constructor. */
	public LineRange(int start, int end) {
		this.start = start;
		this.end = end;
	}

	/** Adjusts either the start or end of the range to include the given line afterwards. */
	public void adjustToContain(int line) {
		if (start > line || end == ISourceNode.UNKNOWN_LINE) {
			start = line;
		}
		if (end < line) {
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

	@Override
	public String toString() {
		if (start == end) {
			return String.valueOf(start);
		} else {
			return start + "-" + end;
		}
	}

	@Override
	public int compareTo(LineRange o) {
		return this.start - o.start;
	}
}
