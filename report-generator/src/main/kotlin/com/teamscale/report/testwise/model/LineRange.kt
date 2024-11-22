package com.teamscale.report.testwise.model

/** Holds a line range with start and end (both inclusive and 1-based).  */
data class LineRange(
	private val start: Int,
	var end: Int
) {
	/**
	 * Returns the line range as used in the XML report.
	 * A range is returned as e.g. 2-5 or simply 3 if the start and end are equal.
	 */
	override fun toString() =
		if (start == end) {
			start.toString()
		} else {
			"$start-$end"
		}
}
