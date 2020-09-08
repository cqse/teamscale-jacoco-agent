package com.teamscale.client;

import com.squareup.moshi.Json;

/**
 * {@link TestDetails} with information about their partition as well as
 * tracking data used during prioritization of tests. Two instances are
 * considered equal if the test details are equals.
 */
public class PrioritizableTest {

	/** The uniform path the test (without -test- prefix). */
	public String uniformPath;

	/** The reason the test has been selected. */
	public String selectionReason;

	/** Partition of the test. */
	public String partition;

	/** Duration in ms. May be null if not set. */
	public Long durationInMs;

	/**
	 * The score determined by the TIA algorithm.
	 */
	@Json(name = "currentScore")
	public double score;

	public PrioritizableTest(String uniformPath) {
		this.uniformPath = uniformPath;
	}

}
