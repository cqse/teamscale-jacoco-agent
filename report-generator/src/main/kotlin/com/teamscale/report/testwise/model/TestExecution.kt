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
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * Representation of a single test (method) execution.
 *
 * @param uniformPath The uniform path of the test (method) that was executed.
 * This is an absolute reference that identifies the test uniquely within the Teamscale project.
 * @param durationMillis Duration of the execution in milliseconds.
 * @param result The result of the test execution.
 * @param message Optional message given for test failures (normally contains a stack trace). May be `null`.
 */
data class TestExecution @JvmOverloads constructor(
	@JvmField
	var uniformPath: String? = null,
	@Deprecated("Use durationSeconds instead.")
	var durationMillis: Long = 0L,
	@JvmField
	val result: ETestExecutionResult? = null,
	val message: String? = null,
) : Serializable {

	/** Duration of the execution in seconds. */
	@JsonProperty("duration")
	@JsonAlias("durationSeconds")
	private val duration: Double? = null

	val durationSeconds: Double
		get() = duration ?: (durationMillis / 1000.0)
}
