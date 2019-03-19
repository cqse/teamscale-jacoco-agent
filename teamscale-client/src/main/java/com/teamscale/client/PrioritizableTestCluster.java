package com.teamscale.client;

import java.util.List;

/**
 * A {@link PrioritizableTestCluster} represents an ordered {@link List} of
 * {@link PrioritizableTest}s which should be executed together to avoid
 * overhead. The order of the {@link PrioritizableTest}s is determined by the
 * prioritization of the {@link PrioritizableTest}s w.r.t. to each other.
 *
 * A {@link PrioritizableTestCluster} assumes that possibly resource intensive
 * setup or teardown operations (e.g. a class containing a method annotated with
 * {@code BeforeClass} in JUnit4 or {@code BeforeAll} in JUnit5) can be executed
 * once for a {@link PrioritizableTestCluster} instead of executing them for
 * each {@link PrioritizableTest}.
 */
public class PrioritizableTestCluster {

	/**
	 * The unique cluster id to which all {@link PrioritizableTest}s belong.
	 *
	 * @see ClusteredTestDetails#clusterId
	 */
	public String clusterId;

	/** The {@link PrioritizableTest}s in this cluster. */
	public List<PrioritizableTest> tests;
}
