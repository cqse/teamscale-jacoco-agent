package com.teamscale.tia.client;

import com.teamscale.client.PrioritizableTestCluster;

import java.util.List;

/**
 * Represents a run of prioritized and selected test clusters as reported by the TIA. Use this class to report test
 * start and end events and upload testwise coverage to Teamscale.
 * <p>
 * The caller of this class should retrieve the test clusters to execute from {@link #getPrioritizedClusters()}, run
 * them (in the given order if possible) and report test start and end events via {@link #startTest(String)} )}.
 * <p>
 * After having run all tests, call {@link #endTestRun()} to create a testwise coverage report and upload it to
 * Teamscale.
 */
public class TestRunWithClusteredSuggestions extends TestRun {

	private final List<PrioritizableTestCluster> prioritizedClusters;

	TestRunWithClusteredSuggestions(ITestwiseCoverageAgentApi api,
									List<PrioritizableTestCluster> prioritizedClusters) {
		super(api);
		this.prioritizedClusters = prioritizedClusters;
	}

	public List<PrioritizableTestCluster> getPrioritizedClusters() {
		return prioritizedClusters;
	}
}
