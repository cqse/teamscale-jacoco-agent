package eu.cqse.teamscale.client;

/** Enum of report formats. */
public enum EReportFormat {
	JACOCO("JaCoCo Coverage", "", "jacoco-coverage", "xml"),
	TESTWISE_COVERAGE("Testwise Coverage", "/Tests", "testwise-coverage", "xml"),
	JUNIT("JUnit Test Results", "/Test Results", "junit", "xml"),
	TEST_LIST("Test List", "/Tests", "test-list", "json");

	/** A readable name for the report type. */
	public final String readableName;

	/**
	 * The suffix that should be appended to the partition.
	 * We need this, because Teamscale marks every uniform path as deleted if uploaded to the same partition even if
	 * the upload does touch different type of data. E.g. JUnit upload will remove all file paths uploaded via JaCoCo
	 * coverage. Furthermore test details and testwise coverage need to be in the same partition.
	 */
	public final String partitionSuffix;

	/** Prefix to use when writing the report to the file system. */
	public final String filePrefix;

	/** File extension of the report. */
	public final String extension;

	EReportFormat(String readableName, String partitionSuffix, String filePrefix, String extension) {
		this.readableName = readableName;
		this.partitionSuffix = partitionSuffix;
		this.filePrefix = filePrefix;
		this.extension = extension;
	}
}
