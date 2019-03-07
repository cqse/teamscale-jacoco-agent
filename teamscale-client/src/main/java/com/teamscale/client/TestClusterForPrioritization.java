package com.teamscale.client;

import java.util.List;

/**
 * Represents a set of tests that should be executed together. For example all impacted tests of a class should be
 * executed together to avoid duplicate setup and teardown logic execution.
 */
public class TestClusterForPrioritization {

	/** Unique id of the cluster. Could be a fully qualified class name for example.  */
	public String clusterId;

	/** The prioritized tests in the test cluster. */
	public List<TestForPrioritization> testsForPrioritization;
}
