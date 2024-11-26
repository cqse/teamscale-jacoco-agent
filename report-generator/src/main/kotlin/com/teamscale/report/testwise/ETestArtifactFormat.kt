package com.teamscale.report.testwise

/** Enum of test artifacts that can be converted to a full testwise coverage report later on.  */
enum class ETestArtifactFormat(
	/** A readable name for the report type.  */
	val readableName: String,
	/** Prefix to use when writing the report to the file system.  */
	val filePrefix: String,
	/** File extension of the report.  */
	val extension: String
) {
	/** A json list of tests ([com.teamscale.client.TestDetails]).  */
	TEST_LIST("Test List", "test-list", "json"),

	/** A json list of test executions ([com.teamscale.report.testwise.model.TestExecution]).  */
	TEST_EXECUTION("Test Execution", "test-execution", "json"),

	/** Binary jacoco test coverage (.exec file).  */
	JACOCO("Jacoco", "", "exec"),

	/** Google closure coverage files with additional uniformPath entries.  */
	CLOSURE("Closure Coverage", "closure-coverage", "json")
}
