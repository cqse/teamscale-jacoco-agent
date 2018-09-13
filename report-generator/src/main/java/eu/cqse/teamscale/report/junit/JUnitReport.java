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

	/** Returns whether the report contains any tests. */
	public boolean isEmpty() {
		return testCaseList.isEmpty();
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

		/** Information about the test error. If it is null the test did not fail. */
		@XmlElement(name = "error")
		private Error error = null;

		/**
		 * Whether the test has been ignored. Null indicates that the flag has not been set,
		 * which basically means false. But results in not getting the attribute in the XML.
		 */
		@XmlAttribute(name = "ignored")
		private Boolean ignored = null;

		/** Whether the test has been skipped. */
		@XmlAttribute(name = "skipped")
		private Skipped skipped = null;

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
		public void setFailure(String failure) {
			this.failure = new TestCase.Failure(failure);
		}

		/** @see #failure */
		public void setError(String error) {
			this.error = new TestCase.Error(error);
		}

		/** @see #ignored */
		public void setIgnored(Boolean ignored) {
			this.ignored = ignored;
		}

		/** @see #skipped */
		public void setSkipped(String skipReason) {
			this.skipped = new Skipped(skipReason);
		}

		/** Container for a failure message/stacktrace etc. */
		public static class Failure {

			/** The actual failure. */
			@XmlValue
			private final String failureOutput;

			/** Constructor. */
			public Failure(String failure) {
				failureOutput = failure;
			}
		}

		/** Container for an error message/stacktrace etc. */
		public static class Error {

			/** The actual error. */
			@XmlValue
			private final String errorOutput;

			/** Constructor. */
			public Error(String error) {
				errorOutput = error;
			}
		}

		/** Container for a skip reason */
		public static class Skipped {

			/** The reason the test was skipped. */
			@XmlValue
			private final String skipReason;

			/** Constructor. */
			public Skipped(String skipReason) {
				this.skipReason = skipReason;
			}
		}
	}
}
