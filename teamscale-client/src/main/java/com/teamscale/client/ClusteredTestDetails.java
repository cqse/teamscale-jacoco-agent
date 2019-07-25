package com.teamscale.client;

/**
 * {@link TestDetails} with additional information about which cluster of tests
 * the test case belongs to during prioritization.
 */
public class ClusteredTestDetails extends TestDetails {

	/**
	 * A unique identifier for the cluster this test should be prioritized within.
	 * If null the test get's assigned it's own cluster.
	 */
	public String clusterId;

	public ClusteredTestDetails(String uniformPath, String sourcePath, String content, String clusterId) {
		super(uniformPath, sourcePath, content);
		this.clusterId = clusterId;
	}
}

