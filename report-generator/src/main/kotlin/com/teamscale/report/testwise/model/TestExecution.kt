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

import java.io.Serializable

/** Representation of a single test (method) execution.  */
class TestExecution @JvmOverloads constructor(
    /**
     * The uniform path of the test (method) that was executed. This is an absolute
     * (i.e. hierarchical) reference which identifies the test uniquely in the scope
     * of the Teamscale project. It may (but is not required to) correspond to the
     * path of some automated test case source code known to Teamscale. If the test
     * was parameterized, this path is expected to reflect the parameter in some
     * manner.
     */
    val uniformPath: String,
    /** Duration of the execution in milliseconds.  */
    /** @see .durationMillis
     */
    val durationMillis: Long,
    /** The actual execution result state.  */
    val result: ETestExecutionResult,
    /**
     * Optional message given for test failures (normally contains a stack trace).
     * May be `null`.
     */
    /** @see .message
     */
    val message: String? = null
) : Serializable {

    /** @see .durationMillis
     */
    val durationSeconds: Double
        get() = durationMillis / 1000.0

    companion object {

        private const val serialVersionUID = 1L
    }
}
