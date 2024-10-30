package com.teamscale.report.testwise.model

import com.teamscale.report.testwise.model.builder.TestCoverageBuilder

/** Container for coverage produced by multiple tests.  */
class TestwiseCoverage {
	/** A mapping from test ID to [TestCoverageBuilder].  */
	val tests: MutableMap<String, TestCoverageBuilder> = HashMap()

	/**
	 * Adds the [TestCoverageBuilder] to the map.
	 * If there is already a test with the same ID the coverage is merged.
	 */
	fun add(coverage: TestCoverageBuilder?) {
		if (coverage == null || coverage.isEmpty) {
			return
		}
		if (tests.containsKey(coverage.uniformPath)) {
			val testCoverage: TestCoverageBuilder? = tests.get(coverage.uniformPath)
			testCoverage!!.addAll(coverage.files)
		} else {
			tests.put(coverage.uniformPath, coverage)
		}
	}

	/**
	 * Merges the given [TestwiseCoverage] with this one.
	 */
	fun add(testwiseCoverage: TestwiseCoverage?) {
		if (testwiseCoverage == null) {
			return
		}
		for (value: TestCoverageBuilder? in testwiseCoverage.tests.values) {
			this.add(value)
		}
	}
}
