package com.teamscale.report.testwise.model.builder;

import com.teamscale.client.TestDetails;
import com.teamscale.report.testwise.model.ETestExecutionResult;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;

/** Generic container of all information about a specific test including details, execution info and coverage. */
public class TestInfoBuilder {

	/** Unique name of the test case by using a path like hierarchical description, which can be shown in the UI. */
	public final String uniformPath;

	/**
	 * Path to the source of the method. Will be equal to uniformPath in most cases, but e.g. @Test methods in a base
	 * class will have the sourcePath pointing to the base class which contains the actual implementation whereas
	 * uniformPath will contain the the class name of the most specific subclass, from where it was actually executed.
	 */
	private String sourcePath = null;

	/**
	 * Some kind of content to tell whether the test specification has changed. Can be revision number or hash over the
	 * specification or similar.
	 */
	private String content = null;

	/** Duration of the execution in milliseconds. */
	private Double durationSeconds = null;

	/** The actual execution result state. */
	private ETestExecutionResult result;

	/**
	 * Optional message given for test failures (normally contains a stack trace). May be {@code null}.
	 */
	private String message;

	/** Coverage generated by this test. */
	private TestCoverageBuilder coverage;

	/** Constructor. */
	/* package */
	public TestInfoBuilder(String uniformPath) {
		this.uniformPath = uniformPath;
	}

	/** @see #uniformPath */
	public String getUniformPath() {
		return uniformPath;
	}

	/** Returns true if there is no coverage for the test yet. */
	public boolean isEmpty() {
		return coverage.isEmpty();
	}

	/** Sets the test details fields. */
	public void setDetails(TestDetails details) {
		if (details != null) {
			sourcePath = details.sourcePath;
			content = details.content;
		}
	}

	/** Sets the test execution fields. */
	public void setExecution(TestExecution execution) {
		if (execution != null) {
			durationSeconds = execution.getDurationSeconds();
			result = execution.getResult();
			message = execution.getMessage();
		}
	}

	/** @see #coverage */
	public void setCoverage(TestCoverageBuilder coverage) {
		this.coverage = coverage;
	}

	/** Builds a {@link TestInfo} object of the data in this container. */
	public TestInfo build() {
		TestInfo testInfo = new TestInfo(uniformPath, sourcePath, content, durationSeconds, result, message);
		if (coverage != null) {
			testInfo.paths.addAll(coverage.getPaths());
		}
		return testInfo;
	}
}
