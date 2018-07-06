package eu.cqse.teamscale.jacoco.agent.testimpact;

/** The result of a test execution. */
public enum ETestExecutionResult {

	/** Test execution was successful. */
	PASSED,

	/** The test is currently marked as "do not execute" (e.g. JUnit @Ignore). */
	IGNORED,

	/** Caused by a failing assumption. */
	SKIPPED,

	/** Caused by a failing assertion. */
	FAILURE,

	/** Caused by an error during test execution (e.g. exception thrown). */
	ERROR
}
