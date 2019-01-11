package com.teamscale.report.testwise;

/** Enum of test artifacts that can be converted to a full testwise coverage report later on. */
public enum ETestArtifactFormat {

	/** A json list of tests ({@link com.teamscale.client.TestDetails}). */
	TEST_LIST("Test List", "test-list", "json"),

	/** A json list of test executions ({@link com.teamscale.report.testwise.model.TestExecution}). */
	TEST_EXECUTION("Test Execution", "test-execution", "json"),

	/** Binary jacoco test coverage (.exec file). */
	JACOCO("Jacoco", "", "exec"),

	/** Google closure coverage files with additional uniformPath entries. */
	CLOSURE("Closure Coverage", "closure-coverage", "json");

	/** A readable name for the report type. */
	public final String readableName;

	/** Prefix to use when writing the report to the file system. */
	public final String filePrefix;

	/** File extension of the report. */
	public final String extension;

	ETestArtifactFormat(String readableName, String filePrefix, String extension) {
		this.readableName = readableName;
		this.filePrefix = filePrefix;
		this.extension = extension;
	}
}
