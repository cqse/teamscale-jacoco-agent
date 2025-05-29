package com.teamscale.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * [TestDetails] with additional information about which cluster of tests the test case belongs to during
 * prioritization.
 */
class ClusteredTestDetails @JsonCreator constructor(
	/**
	 * The uniform path of the test case.
	 */
	@JsonProperty("uniformPath") uniformPath: String,

	/**
	 * The source path of the test case, if available.
	 */
	@JsonProperty("sourcePath") sourcePath: String?,
	/**
	 * The content associated with the test case, if available.
	 */
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
	) var partition: String
) : TestDetails(uniformPath, sourcePath, content)

