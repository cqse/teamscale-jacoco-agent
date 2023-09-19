package com.teamscale.client;

import com.squareup.moshi.Json;

import java.util.StringJoiner;

/**
 * {@link TestDetails} with information about their partition as well as tracking data used during prioritization of
 * tests. Two instances are considered equal if the test details are equals.
 */
public class PrioritizableTest {

	/** The uniform path without the "-test-execution" or "-execution-unit-" prefix */
	public String testName;

	/** The uniform path of the test including the "-test-execution" or "-execution-unit-" prefix. */
	public String uniformPath;

	/** The reason the test has been selected. */
	public String selectionReason;

	/** Partition of the test. */
	public String partition;

	/**
	 * Duration in ms. May be null if not set. This can happen when the uploaded testwise coverage data does not include
	 * duration information or for new tests that have not been executed yet.
	 */
	public Long durationInMs;

	/**
	 * The score determined by the TIA algorithm. The value is guaranteed to be positive. Higher values describe a
	 * higher probability of the test to detect potential bugs. The value can only express a relative importance
	 * compared to other scores of the same request. It makes no sense to compare the score against absolute values.
	 */
	@Json(name = "currentScore")
	public double score;

	/**
	 * Field for storing the tests rank. The rank is the 1-based index of the test in the prioritized list.
	 */
	@Json(name = "rank")
	public int rank;

	public PrioritizableTest(String testName) {
		this.testName = testName;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", PrioritizableTest.class.getSimpleName() + "[", "]")
				.add("testName='" + testName + "'")
				.add("uniformPath='" + uniformPath + "'")
				.add("selectionReason='" + selectionReason + "'")
				.add("partition='" + partition + "'")
				.add("durationInMs=" + durationInMs)
				.add("score=" + score)
				.add("rank=" + rank)
				.toString();
	}
}
