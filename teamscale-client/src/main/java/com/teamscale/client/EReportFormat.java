package com.teamscale.client;

/** Enum of report formats. */
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
