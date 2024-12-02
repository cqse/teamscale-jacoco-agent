package com.teamscale.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * A [PrioritizableTestCluster] represents an ordered [List] of [PrioritizableTest]s which should be
 * executed together to avoid overhead. The order of the [PrioritizableTest]s is determined by the prioritization
 * of the [PrioritizableTest]s w.r.t. to each other.
 *
 *
 * A [PrioritizableTestCluster] assumes that possibly resource intensive setup or teardown operations (e.g. a
 * class containing a method annotated with `BeforeClass` in JUnit4 or `BeforeAll` in JUnit5) can be
 * executed once for a [PrioritizableTestCluster] instead of executing them for each [PrioritizableTest].
 */
class PrioritizableTestCluster @JsonCreator constructor(
	/**
	 * The unique cluster id to which all [PrioritizableTest]s belong.
	 *
	 * @see ClusteredTestDetails.clusterId
	 */
	@param:JsonProperty("clusterId") var clusterId: String,
	/** The [PrioritizableTest]s in this cluster.  */
	@JvmField @param:JsonProperty("tests") var tests: List<PrioritizableTest>?
) {
	/**
	 * The score determined by the TIA algorithm. The value is guaranteed to be positive. Higher values describe a
	 * higher probability of the test to detect potential bugs. The value can only express a relative importance
	 * compared to other scores of the same request. It makes no sense to compare the score against absolute values.
	 * The value is 0 if no availableTests are given.
	 */
	@JsonProperty("currentScore")
	var score = 0.0

	/**
	 * Field for storing the tests rank. The rank is the 1-based index of the test
	 * in the prioritized list.
	 */
	var rank: Int = 0

	override fun toString() =
		StringJoiner(", ", PrioritizableTestCluster::class.java.simpleName + "[", "]")
			.add("clusterId='$clusterId'")
			.add("score=$score")
			.add("rank=$rank")
			.add("tests=$tests")
			.toString()
}
