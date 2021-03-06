package com.teamscale.client;

/**
 * Enum of report formats.
 * This is the subset of the report formats supported by Teamscale that the plugin currently implements support for.
 * See https://docs.teamscale.com/reference/upload-formats-and-samples/#supported-formats-for-upload
 */
public enum EReportFormat {
	JACOCO("JaCoCo Coverage"),
	JUNIT("JUnit Results"),
	TESTWISE_COVERAGE("Testwise Coverage");

	/** A readable name for the report type. */
	public final String readableName;

	EReportFormat(String readableName) {
		this.readableName = readableName;
	}
}
