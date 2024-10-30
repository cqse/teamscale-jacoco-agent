package com.teamscale.report.testwise.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/** Generic container of all information about a specific test as written to the report.  */
class TestInfo @JsonCreator constructor(
	/** Unique name of the test case by using a path like hierarchical description, which can be shown in the UI.  */
	@JvmField @param:JsonProperty("uniformPath") val uniformPath: String,
	/**
	 * Path to the source of the method. Will be equal to uniformPath in most cases, but e.g. @Test methods in a Base
	 * class will have the sourcePath pointing to the Base class which contains the actual implementation whereas
	 * uniformPath will contain the class name of the most specific subclass, from where it was actually executed.
	 */
	@param:JsonProperty("sourcePath") val sourcePath: String?,
	/**
	 * Some kind of content to tell whether the test specification has changed. Can be revision number or hash over the
	 * specification or similar.
	 */
	@param:JsonProperty("content") val content: String?,
	/** Duration of the execution in seconds.  */
	@param:JsonProperty("duration") val duration: Double?,
	/** The actual execution result state.  */
	@JvmField @param:JsonProperty("result") val result: ETestExecutionResult?,
	/**
	 * Optional message given for test failures (normally contains a stack trace). May be `null`.
	 */
	@param:JsonProperty("message") val message: String?
) {
	/** All paths that the test did cover.  */
	@JvmField
	val paths: MutableList<PathCoverage> = ArrayList()
}
