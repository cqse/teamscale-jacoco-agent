/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2005-2018 The ConQAT Project                               |
|                                                                          |
| Licensed under the Apache License, Version 2.0 (the "License");          |
| you may not use this file except in compliance with the License.         |
| You may obtain a copy of the License at                                  |
|                                                                          |
|    http://www.apache.org/licenses/LICENSE-2.0                            |
|                                                                          |
| Unless required by applicable law or agreed to in writing, software      |
| distributed under the License is distributed on an "AS IS" BASIS,        |
| WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. |
| See the License for the specific language governing permissions and      |
| limitations under the License.                                           |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.testwise.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/** Representation of a single test (method) execution.  */
class TestExecution : Serializable {
	/** @see .uniformPath
	 */
	/** @see .uniformPath
	 */
	/**
	 * The uniform path of the test (method) that was executed. This is an absolute (i.e. hierarchical) reference which
	 * identifies the test uniquely in the scope of the Teamscale project. It may (but is not required to) correspond to
	 * the path of some automated test case source code known to Teamscale. If the test was parameterized, this path is
	 * expected to reflect the parameter in some manner.
	 */
	@JvmField
	var uniformPath: String? = null

	/** Duration of the execution in milliseconds.  */
	@Deprecated("")
	private var durationMillis: Long = 0

	/** Duration of the execution in seconds.  */
	@JsonProperty("duration")
	@JsonAlias("durationSeconds")
	private val duration: Double? = null

	/** @see .result
	 */
	/** @see .result
	 */
	/** The actual execution result state.  */
	@JvmField
	var result: ETestExecutionResult? = null

	/** @see .message
	 */
	/** @see .message
	 */
	/**
	 * Optional message given for test failures (normally contains a stack trace). May be `null`.
	 */
	var message: String? = null

	/**
	 * Needed for Jackson deserialization.
	 */
	@JsonCreator
	constructor()

	@JvmOverloads
	constructor(name: String?, durationMillis: Long, result: ETestExecutionResult?, message: String? = null) {
		this.uniformPath = name
		this.durationMillis = durationMillis
		this.result = result
		this.message = message
	}

	val durationSeconds: Double
		/** @see .durationMillis
		 */
		get() {
			if (duration != null) {
				return duration
			} else {
				return durationMillis / 1000.0
			}
		}

	/** @see .durationMillis
	 */
	fun setDurationMillis(durationMillis: Long) {
		this.durationMillis = durationMillis
	}

	override fun toString(): String {
		return "TestExecution{" +
				"uniformPath='" + uniformPath + '\'' +
				", durationMillis=" + durationMillis +
				", duration=" + duration +
				", result=" + result +
				", message='" + message + '\'' +
				'}'
	}

	companion object {
		private const val serialVersionUID: Long = 1L
	}
}
