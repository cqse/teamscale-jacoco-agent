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
package com.teamscale.report.testwise.model;

import java.io.Serializable;

/** Representation of a single test (method) execution. */
public class TestExecution implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The uniform path of the test (method) that was executed. This is an absolute (i.e. hierarchical) reference which
	 * identifies the test uniquely in the scope of the Teamscale project. It may (but is not required to) correspond to
	 * the path of some automated test case source code known to Teamscale. If the test was parameterized, this path is
	 * expected to reflect the parameter in some manner.
	 */
	private String uniformPath;

	/** Duration of the execution in milliseconds. */
	@Deprecated
	private long durationMillis;

	/** Duration of the execution in seconds. */
	private Double duration;

	/** The actual execution result state. */
	private ETestExecutionResult result;

	/**
	 * Optional message given for test failures (normally contains a stack trace). May be {@code null}.
	 */
	private String message;

	public TestExecution(String name, long durationMillis, ETestExecutionResult result) {
		this(name, durationMillis, result, null);
	}

	public TestExecution(String name, long durationMillis, ETestExecutionResult result, String message) {
		this.uniformPath = name;
		this.durationMillis = durationMillis;
		this.result = result;
		this.message = message;
	}

	/** @see #durationMillis */
	public double getDurationSeconds() {
		if (duration != null) {
			return duration;
		} else {
			return durationMillis / 1000.0;
		}
	}

	/** @see #result */
	public ETestExecutionResult getResult() {
		return result;
	}

	/** @see #message */
	public String getMessage() {
		return message;
	}

	/** @see #uniformPath */
	public String getUniformPath() {
		return uniformPath;
	}

	/** @see #uniformPath */
	public void setUniformPath(String uniformPath) {
		this.uniformPath = uniformPath;
	}

	/** @see #durationMillis */
	public void setDurationMillis(long durationMillis) {
		this.durationMillis = durationMillis;
	}

	/** @see #result */
	public void setResult(ETestExecutionResult result) {
		this.result = result;
	}

	/** @see #message */
	public void setMessage(String message) {
		this.message = message;
	}
}
