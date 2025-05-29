package com.teamscale.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Test with additional information about which cluster of tests the test case belongs to during prioritization.
 */
class TestWithClusterId @JsonCreator constructor(
	/**
	 * The uniform path of the test (unescaped and without -test-execution- prefix).
	 */
	@param:JsonProperty("testName") val testName: String,
	/**
	 * The hashed content of the test.
	 */
	@param:JsonProperty("hash") val hash: String?,
	/**
	 * The partition of the test.
	 */
	@param:JsonProperty("partition") val partition: String,
	/**
	 * A unique identifier for the cluster this test should be prioritized within. May not be null.
	 */
	@param:JsonProperty("clusterId") val clusterId: String
) {
	companion object {
		/**
		 * Creates a #TestWithClusterId from a #ClusteredTestDetails object.
		 */
		fun fromClusteredTestDetails(clusteredTestDetails: ClusteredTestDetails) =
			TestWithClusterId(
				clusteredTestDetails.uniformPath, clusteredTestDetails.content,
				clusteredTestDetails.partition, clusteredTestDetails.clusterId ?: clusteredTestDetails.uniformPath
			)
	}
}
