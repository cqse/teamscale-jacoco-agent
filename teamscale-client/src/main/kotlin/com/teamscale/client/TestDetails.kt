package com.teamscale.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * Contains details about a test.
 */
open class TestDetails @JsonCreator constructor(
	/** Unique name of the test case by using a path like hierarchical description, which can be shown in the UI.  */
	@JvmField @param:JsonProperty("uniformPath") var uniformPath: String,
	/**
	 * Path to the source of the method. Will be equal to uniformPath in most cases, but e.g. @Test methods in a base
	 * class will have the sourcePath pointing to the base class which contains the actual implementation whereas
	 * uniformPath will contain the the class name of the most specific subclass, from where it was actually executed.
	 */
	@JvmField @param:JsonProperty("sourcePath") var sourcePath: String?,
	/**
	 * Some kind of content to tell whether the test specification has changed. Can be revision number or hash over the
	 * specification or similar. You can include e.g. a hash of each test's test data so that whenever the test data
	 * changes, the corresponding test is re-run.
	 */
	@param:JsonProperty("content") var content: String?
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other == null || javaClass != other.javaClass) {
			return false
		}
		val that = other as TestDetails
		return uniformPath == that.uniformPath &&
				sourcePath == that.sourcePath &&
				content == that.content
	}

	override fun hashCode() = Objects.hash(uniformPath, sourcePath, content)
}
