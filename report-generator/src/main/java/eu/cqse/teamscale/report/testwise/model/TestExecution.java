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
package eu.cqse.teamscale.report.testwise.model;

import org.conqat.lib.commons.string.StringUtils;

import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;

/** Representation of a single test (method) execution. */
public class TestExecution implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The uniform path of the test (method) that was executed. This is an absolute
	 * (i.e. hierarchical) reference which identifies the test uniquely in the scope
	 * of the Teamscale project. It may (but is not required to) correspond to the
	 * path of some automated test case source code known to Teamscale. If the test
	 * was parameterized, this path is expected to reflect the parameter in some
	 * manner.
	 */
	private final String uniformPath;

	/** Duration of the execution in milliseconds. */
	private final double durationMillis;

	/** The actual execution result state. */
	private final ETestExecutionResult result;

	/**
	 * Optional message given for test failures (normally contains a stack trace).
	 * May be {@code null}.
	 */
	private final String message;

	public TestExecution(String name, double durationMillis, ETestExecutionResult result) {
		this(name, durationMillis, result, null);
	}

	public TestExecution(String name, double durationMillis, ETestExecutionResult result, String message) {
		this.uniformPath = name;
		this.durationMillis = durationMillis;
		this.result = result;
		this.message = message;
	}

	/** @see #durationMillis */
	public double getDurationMillis() {
		return durationMillis;
	}

	public ETestExecutionResult getResult() {
		return result;
	}

	/** @see #message */
	public Message getMessage() {
		if (StringUtils.isEmpty(message)) {
			return null;
		}
		return new Message(message);
	}

	public String getUniformPath() {
		return uniformPath;
	}

	/** Container for a failure message/stacktrace etc. or skip reason */
	public static class Message {

		/** The actual message. */
		@XmlValue
		private final String message;

		/** Constructor. */
		public Message(String message) {
			this.message = message;
		}
	}
}
