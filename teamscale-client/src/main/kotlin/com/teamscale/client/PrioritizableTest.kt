package com.teamscale.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * [TestDetails] with information about their partition as well as tracking data used during prioritization of
 * tests. Two instances are considered equal if the test details are equals.
 */
class PrioritizableTest @JsonCreator constructor(
	/** The uniform path without the "-test-execution" or "-execution-unit-" prefix  */
	@JvmField @param:JsonProperty("testName") var testName: String
) {
	/** The uniform path of the test including the "-test-execution" or "-execution-unit-" prefix.  */
	var uniformPath: String? = null

	/** The reason the test has been selected.  */
	var selectionReason: String? = null

	/** Partition of the test.  */
	var partition: String? = null

	/**
	 * Duration in ms. May be null if not set. This can happen when the uploaded testwise coverage data does not include
	 * duration information or for new tests that have not been executed yet.
	 */
	var durationInMs: Long? = null

	/**
	 * The score determined by the TIA algorithm. The value is guaranteed to be positive. Higher values describe a
	 * higher probability of the test to detect potential bugs. The value can only express a relative importance
	 * compared to other scores of the same request. It makes no sense to compare the score against absolute values.
	 */
	@JsonProperty("currentScore")
	var score: Double = 0.0

	/**
	 * Field for storing the tests rank. The rank is the 1-based index of the test in the prioritized list.
	 */
	var rank: Int = 0

	override fun toString(): String {
		return StringJoiner(", ", PrioritizableTest::class.java.simpleName + "[", "]")
			.add("testName='$testName'")
			.add("uniformPath='$uniformPath'")
			.add("selectionReason='$selectionReason'")
			.add("partition='$partition'")
			.add("durationInMs=$durationInMs")
			.add("score=$score")
			.add("rank=$rank")
			.toString()
	}
}
