package eu.cqse.teamscale.client;

/** Enum of report formats. */
public enum EReportFormat {
	JACOCO("JaCoCo Coverage", "jacoco-coverage", "xml"),
	TESTWISE_COVERAGE("Testwise Coverage", "testwise-coverage", "json"),
	TEST_LIST("Test List", "test-list", "json"),
	TEST_EXECUTION("Test Execution", "test-execution", "json");

	/** A readable name for the report type. */
	public final String readableName;

	/** Prefix to use when writing the report to the file system. */
	public final String filePrefix;

	/** File extension of the report. */
	public final String extension;

	EReportFormat(String readableName, String filePrefix, String extension) {
		this.readableName = readableName;
		this.filePrefix = filePrefix;
		this.extension = extension;
	}
}
