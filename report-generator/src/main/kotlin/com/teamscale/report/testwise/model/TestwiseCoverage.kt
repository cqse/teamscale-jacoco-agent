package com.teamscale.report.testwise.model

import com.teamscale.report.testwise.model.builder.TestCoverageBuilder

/** Container for coverage produced by multiple tests.  */
class TestwiseCoverage {
	/** A mapping from test ID to [TestCoverageBuilder].  */
	val tests = mutableMapOf<String, TestCoverageBuilder>()

	/**
	 * Adds the [TestCoverageBuilder] to the map.
	 * If there is already a test with the same ID the coverage is merged.
	 */
	fun add(coverage: TestCoverageBuilder?) {
		if (coverage == null) return
		if (tests.containsKey(coverage.uniformPath)) {
			tests[coverage.uniformPath]?.addAll(coverage.files)
		} else {
			tests[coverage.uniformPath] = coverage
		}
	}

	/**
	 * Merges the given [TestwiseCoverage] with this one.
	 */
	fun add(testwiseCoverage: TestwiseCoverage?) {
		if (testwiseCoverage == null) return
		testwiseCoverage.tests.values.forEach { value ->
			add(value)
		}
	}
}
