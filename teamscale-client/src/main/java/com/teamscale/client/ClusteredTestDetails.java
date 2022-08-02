package com.teamscale.client;

/**
 * {@link TestDetails} with additional information about which cluster of tests the test case belongs to during
 * prioritization.
 */
public class ClusteredTestDetails extends TestDetails {

	/**
	 * A unique identifier for the cluster this test should be prioritized within. If null the test gets assigned it's
	 * own unique cluster.
	 */
	public String clusterId;

	/**
	 * The partition for the cluster this test should be prioritized within and the result will be uploaded to. If null
	 * all partitions will be considered.
	 */
	public String partition;

	public ClusteredTestDetails(String uniformPath, String sourcePath, String content, String clusterId,
								String partition) {
		super(uniformPath, sourcePath, content);
		this.clusterId = clusterId;
		this.partition = partition;
	}

}

