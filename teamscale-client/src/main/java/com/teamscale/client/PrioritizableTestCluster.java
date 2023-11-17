package com.teamscale.client;

import com.squareup.moshi.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * A {@link PrioritizableTestCluster} represents an ordered {@link List} of {@link PrioritizableTest}s which should be
 * executed together to avoid overhead. The order of the {@link PrioritizableTest}s is determined by the prioritization
 * of the {@link PrioritizableTest}s w.r.t. to each other.
 * <p>
 * A {@link PrioritizableTestCluster} assumes that possibly resource intensive setup or teardown operations (e.g. a
 * class containing a method annotated with {@code BeforeClass} in JUnit4 or {@code BeforeAll} in JUnit5) can be
 * executed once for a {@link PrioritizableTestCluster} instead of executing them for each {@link PrioritizableTest}.
 */
public class PrioritizableTestCluster {

	/**
	 * The unique cluster id to which all {@link PrioritizableTest}s belong.
	 *
	 * @see ClusteredTestDetails#clusterId
	 */
	public String clusterId;

	/**
	 * The score determined by the TIA algorithm. The value is guaranteed to be positive. Higher values describe a
	 * higher probability of the test to detect potential bugs. The value can only express a relative importance
	 * compared to other scores of the same request. It makes no sense to compare the score against absolute values.
	 * The value is 0 if no availableTests are given.
	 */
	@Json(name = "currentScore")
	public double score;

	/**
	 * Field for storing the tests rank. The rank is the 1-based index of the test
	 * in the prioritized list.
	 */
	@Json(name = "rank")
	public int rank;

	/** The {@link PrioritizableTest}s in this cluster. */
	public List<PrioritizableTest> tests;

	@SuppressWarnings("unused")
		// Moshi might use this (TS-36477)
	PrioritizableTestCluster() {
		this("", new ArrayList<>());
	}

	public PrioritizableTestCluster(String clusterId, List<PrioritizableTest> tests) {
		this.clusterId = clusterId;
		this.tests = tests;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", PrioritizableTestCluster.class.getSimpleName() + "[", "]")
				.add("clusterId='" + clusterId + "'")
				.add("score=" + score)
				.add("rank=" + rank)
				.add("tests=" + tests)
				.toString();
	}
}
