package eu.cqse.teamscale.report.junit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.util.ArrayList;
import java.util.List;

/** Container for a JUnit test report. */
@XmlRootElement(name = "testsuite")
public class JUnitReport {

	/** List of test cases contained in the test suite. */
	@XmlElement(name = "testcase")
	private final List<TestCase> testCaseList = new ArrayList<>();

	/** Adds a test case to the list. */
	public void add(TestCase testCase) {
		testCaseList.add(testCase);
	}

	/** Resets the report model to contain no tests. */
	public void reset() {
		testCaseList.clear();
	}

	/** Holds execution information for a single test case. */
	public static class TestCase {

		/** The fully qualified class name of the test class. */
		@XmlAttribute(name = "classname")
		private final String className;

		/** The name of the test case */
		@XmlAttribute(name = "name")
		private final String testName;

		/** Test duration in seconds. */
		private double durationInSeconds;

		/** Information about the test failure. If it is null the test did not fail. */
		@XmlElement(name = "failure")
		private Failure failure = null;

		/**
		 * Whether the test has been skipped/ignored. Null indicates that the flag has not been set,
		 * which basically means false. But results in not getting the attribute in the XML.
		 */
		@XmlAttribute(name = "ignored")
		private Boolean ignored = null;

		/** Constructor. */
		public TestCase(String className, String testName) {
			this.className = className;
			this.testName = testName;
		}

		/** Returns the duration in seconds as string with 3 trailing digits. */
		@XmlAttribute(name = "time")
		public String getDurationInSeconds() {
			return String.format("%.3f", durationInSeconds);
		}

		/** @see #durationInSeconds */
		public void setDurationInSeconds(double durationInSeconds) {
			this.durationInSeconds = durationInSeconds;
		}

		/** @see #failure */
		public void setFailure(Failure failure) {
			this.failure = failure;
		}

		/** @see #ignored */
		public void setIgnored(Boolean ignored) {
			this.ignored = ignored;
		}

		/** Container for an error message/stacktrace etc. */
		public static class Failure {

			/** The actual failure. */
			@XmlValue
			private final String failureOutput;

			/** Constructor. */
			public Failure(String failure) {
				failureOutput = failure;
			}
		}
	}
}
