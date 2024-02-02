package com.teamscale.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

	@JsonCreator
	public TestWithClusterId(@JsonProperty("testName") String testName, @JsonProperty("hash") String hash,
							 @JsonProperty("partition") String partition, @JsonProperty("clusterId") String clusterId) {
		this.testName = testName;
		this.hash = hash;
		this.partition = partition;
		this.clusterId = clusterId;
	}

	/**
	 * Creates a #TestWithClusterId from a #ClusteredTestDetails object.
	 */
	public static TestWithClusterId fromClusteredTestDetails(ClusteredTestDetails clusteredTestDetails) {
		return new TestWithClusterId(clusteredTestDetails.uniformPath, clusteredTestDetails.content,
				clusteredTestDetails.partition, clusteredTestDetails.clusterId);
	}
}
