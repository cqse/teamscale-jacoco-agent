package com.teamscale.report.testwise.model

import com.teamscale.report.testwise.model.builder.TestCoverageBuilder
import java.util.HashMap

/** Container for coverage produced by multiple tests.  */
class TestwiseCoverage {

    /** A mapping from test ID to [TestCoverageBuilder].  */
    private val tests = HashMap<String, TestCoverageBuilder>()

    /**
     * Adds the [TestCoverageBuilder] to the map.
     * If there is already a test with the same ID the coverage is merged.
     */
    fun add(coverage: TestCoverageBuilder?) {
        if (coverage == null || coverage.isEmpty) {
            return
        }
        if (tests.containsKey(coverage.uniformPath)) {
            val testCoverage = tests[coverage.uniformPath]
            testCoverage!!.addAll(coverage.files)
        } else {
            tests[coverage.uniformPath] = coverage
        }
    }

    /**
     * Merges the given [TestwiseCoverage] with this one.
     */
    fun add(testwiseCoverage: TestwiseCoverage?) {
        if (testwiseCoverage == null) {
            return
        }
        for (value in testwiseCoverage.tests.values) {
            this.add(value)
        }
    }

    fun getTests(): Collection<TestCoverageBuilder> {
        return tests.values
    }
}
