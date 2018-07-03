package eu.cqse.teamscale.jacoco.agent.testimpact;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.util.ArrayList;
import java.util.List;

/** Container for a JUnit test report. */
@XmlRootElement(name = "testsuite")
class JUnitReport {

	/** List of test cases contained in the test suite. */
	@XmlElement(name = "testcase")
	List<TestCase> testCaseList = new ArrayList<>();

	public static class TestCase {

		/** The fully qualified class name of the test class. */
		@XmlAttribute(name = "classname")
		public final String className;

		/** The name of the test case */
		@XmlAttribute(name = "name")
		public final String testName;

		/** Test duration in seconds. */
		@XmlAttribute(name = "time")
		public final String durationInSeconds;

		@XmlElement(name = "error")
		public Failure failure = null;

		@XmlAttribute(name = "ignored")
		public Boolean ignored = null;

		public TestCase(String className, String testName, double durationInSeconds) {
			this.className = className;
			this.testName = testName;
			this.durationInSeconds = String.format("%.3f", durationInSeconds);
		}

		/** Container for an error message/stacktrace etc. */
		public static class Failure {

			/** The actual error. */
			@XmlValue
			private String failureOutput;

			/** Constructor. */
			public Failure(String failure) {
				failureOutput = failure;
			}
		}
	}
}
