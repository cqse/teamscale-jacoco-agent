package com.teamscale.report.testwise.model

/** Holds a line range with start and end (both inclusive and 1-based).  */
class LineRange
/** Constructor.  */(
	/** The start line (1-based).  */
	private val start: Int,
	/** The end line (1-based).  */
	var end: Int
) {
	/** @see .end
	 */
	/** @see .end
	 */

	/**
	 * Returns the line range as used in the XML report.
	 * A range is returned as e.g. 2-5 or simply 3 if the start and end are equal.
	 */
	fun toReportString(): String {
		if (start == end) {
			return start.toString()
		} else {
			return start.toString() + "-" + end
		}
	}

	override fun toString(): String {
		return toReportString()
	}
}
