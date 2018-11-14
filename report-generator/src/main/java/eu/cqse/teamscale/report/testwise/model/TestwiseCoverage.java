package eu.cqse.teamscale.report.testwise.model;

import eu.cqse.teamscale.report.testwise.model.builder.TestCoverageBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Container for coverage produced by multiple tests. */
public class TestwiseCoverage {

	/** A mapping from test ID to {@link TestCoverageBuilder}. */
	private final Map<String, TestCoverageBuilder> tests = new HashMap<>();

	/**
	 * Adds the {@link TestCoverageBuilder} to the map.
	 * If there is already a test with the same ID the coverage is merged.
	 */
	public void add(TestCoverageBuilder coverage) {
		if (coverage == null || coverage.isEmpty()) {
			return;
		}
		if (tests.containsKey(coverage.getUniformPath())) {
			TestCoverageBuilder testCoverage = tests.get(coverage.getUniformPath());
			testCoverage.addAll(coverage.getFiles());
		} else {
			tests.put(coverage.getUniformPath(), coverage);
		}
	}

	/**
	 * Merges the given {@link TestwiseCoverage} with this one.
	 */
	public void add(TestwiseCoverage testwiseCoverage) {
		if (testwiseCoverage == null) {
			return;
		}
		for (TestCoverageBuilder value : testwiseCoverage.tests.values()) {
			this.add(value);
		}
	}

	public Collection<TestCoverageBuilder> getTests() {
		return tests.values();
	}
}
