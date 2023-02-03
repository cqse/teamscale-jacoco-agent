package com.teamscale.tia.client;

import com.teamscale.client.PrioritizableTest;

import java.util.List;

/**
 * Represents a run of a flat list of prioritized and selected tests as reported by the TIA. Use this class to report
 * test start and end events and upload testwise coverage to Teamscale.
 * <p>
 * The caller of this class should retrieve the tests to execute from {@link #getPrioritizedTests()}, run them (in the
 * given order if possible) and report test start and end events via {@link #startTest(String)} )}.
 * <p>
 * After having run all tests, call {@link #endTestRun(boolean)} to create a testwise coverage report and upload it to
 * Teamscale.
 */
public class TestRunWithFlatSuggestions extends TestRun {

	private final List<PrioritizableTest> prioritizedTests;

	TestRunWithFlatSuggestions(ITestwiseCoverageAgentApi api,
							   List<PrioritizableTest> prioritizedTests) {
		super(api);
		this.prioritizedTests = prioritizedTests;
	}

	public List<PrioritizableTest> getPrioritizedTests() {
		return prioritizedTests;
	}
}
