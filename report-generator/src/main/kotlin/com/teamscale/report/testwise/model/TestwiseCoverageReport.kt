package com.teamscale.report.testwise.model

import com.fasterxml.jackson.annotation.JsonProperty

/** Container for coverage produced by multiple tests.  */
data class TestwiseCoverageReport(
	/**
	 * If set to `true` the set of tests contained in the report don't represent the full set of tests within a
	 * partition. These tests are added or updated in Teamscale, but no tests or executable units that are missing in
	 * the report will be deleted.
	 */
	@JvmField @param:JsonProperty("partial") val partial: Boolean
) {
	/** The tests contained in the report.  */
	@JvmField
	val tests = mutableListOf<TestInfo>()
}
