package com.teamscale.client;

/** Described all feature toggles of the impacted-tests services. */
public enum ETestImpactOptions {

	/**
	 * Returns impacted tests first and then appends all non-impacted tests.
	 * This always returns all tests, but still allows to fail faster as impacted tests are executed first.
	 */
	INCLUDE_NON_IMPACTED,

	/**
	 * Includes test in the response that did fail or were skipped/ignored in previous test runs even when the
	 * currently inspected changes do not impact those tests. This can be used to ensure a pipeline does not get green
	 * even though there are still known test failures.
	 */
	INCLUDE_FAILED_AND_SKIPPED,

	/**
	 * Ensures that the exact given end commit has been processed by Teamscale. Otherwise results returned from the
	 * impacted-tests service might be incomplete when the analysis did not reach the expected commits yet.
	 */
	ENSURE_PROCESSED
}
