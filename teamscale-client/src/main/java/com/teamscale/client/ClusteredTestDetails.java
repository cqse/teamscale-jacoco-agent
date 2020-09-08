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

	public ClusteredTestDetails(String uniformPath, String sourcePath, String content, String clusterId) {
		super(uniformPath, sourcePath, content);
		this.clusterId = clusterId;
	}

	/**
	 * Creates clustered test details with the given additional {@link TestData}.
	 * <p>
	 * Use this to easily mark additional files or data as belonging to that test case. Whenever the given {@link
	 * TestData} changes, this test will be selected to be run by the TIA.
	 * <p>
	 * Example: For a test that reads test data from an XML file, you should pass the contents of that XML file as its
	 * test data. Then, whenever the XML is modified, the corresponding test will be run by the TIA.
	 */
	public ClusteredTestDetails(String uniformPath, String sourcePath, TestData testData, String clusterId) {
		super(uniformPath, sourcePath, testData.hash);
		this.clusterId = clusterId;
	}

}

