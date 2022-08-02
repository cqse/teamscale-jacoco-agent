package com.teamscale.client;

/**
 * Test with additional information about which cluster of tests the test case belongs to during prioritization.
 */
public class TestWithClusterId {


	/**
	 * The uniform path of the test (unescaped and without -test-execution- prefix).
	 */
	public final String testName;

	/**
	 * The hashed content of the test.
	 */
	public final String hash;

	/**
	 * The partition of the test.
	 */
	public final String partition;

	/**
	 * A unique identifier for the cluster this test should be prioritized within. May not be null.
	 */
	public final String clusterId;

	public TestWithClusterId(String testName, String hash, String partition, String clusterId) {
		this.testName = testName;
		this.hash = hash;
		this.partition = partition;
		this.clusterId = clusterId;
	}

	public static TestWithClusterId fromClusteredTestDetails(ClusteredTestDetails clusteredTestDetails) {
		return new TestWithClusterId(clusteredTestDetails.uniformPath, clusteredTestDetails.content,
				clusteredTestDetails.partition, clusteredTestDetails.clusterId);
	}
}
