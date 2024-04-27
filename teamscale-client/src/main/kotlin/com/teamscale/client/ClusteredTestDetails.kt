package com.teamscale.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * [TestDetails] with additional information about which cluster of tests the test case belongs to during
 * prioritization.
 */
class ClusteredTestDetails @JsonCreator constructor(
	@JsonProperty("uniformPath") uniformPath: String,
	@JsonProperty("sourcePath") sourcePath: String,
	@JsonProperty("content") content: String?,
	/**
	 * A unique identifier for the cluster this test should be prioritized within. If null the test gets assigned its
	 * own unique cluster.
	 */
	@param:JsonProperty(
		"clusterId"
	) var clusterId: String?,
	/**
	 * The partition for the cluster this test should be prioritized within and the result will be uploaded to.
	 */
	@param:JsonProperty(
		"partition"
	) var partition: String?
) : TestDetails(uniformPath, sourcePath, content) {
	companion object {
		/**
		 * Creates clustered test details with the given additional [TestData].
		 *
		 *
		 * Use this to easily mark additional files or data as belonging to that test case. Whenever the given
		 * [TestData] changes, this test will be selected to be run by the TIA.
		 *
		 *
		 * Example: For a test that reads test data from an XML file, you should pass the contents of that XML file as its
		 * test data. Then, whenever the XML is modified, the corresponding test will be run by the TIA.
		 */
		fun createWithTestData(
			uniformPath: String, sourcePath: String, testData: TestData,
			clusterId: String, partition: String
		): ClusteredTestDetails {
			return ClusteredTestDetails(uniformPath, sourcePath, testData.hash, clusterId, partition)
		}
	}
}

