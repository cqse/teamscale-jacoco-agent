package eu.cqse.teamscale.report.testwise.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Container for coverage produced by multiple tests. */
public class TestwiseCoverage {

	/** A mapping from test ID to {@link TestCoverage}. */
	private final Map<String, TestCoverage> tests = new HashMap<>();

	/**
	 * Adds the {@link TestCoverage} to the map.
	 * If there is already a test with the same ID the coverage is merged.
	 */
	public void add(TestCoverage coverage) {
		if (coverage == null || coverage.isEmpty()) {
			return;
		}
		if (tests.containsKey(coverage.getUniformPath())) {
			TestCoverage testCoverage = tests.get(coverage.getUniformPath());
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
		for (TestCoverage value : testwiseCoverage.tests.values()) {
			this.add(value);
		}
	}

	public Collection<TestCoverage> getTests() {
		return tests.values();
	}
}
