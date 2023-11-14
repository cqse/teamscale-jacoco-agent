package com.teamscale.report.testwise.model;

import java.util.ArrayList;
import java.util.List;

/** Generic container of all information about a specific test as written to the report. */
@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
public class TestInfo {

	/** Unique name of the test case by using a path like hierarchical description, which can be shown in the UI. */
	public final String uniformPath;

	/**
	 * Path to the source of the method. Will be equal to uniformPath in most cases, but e.g. @Test methods in a Base
	 * class will have the sourcePath pointing to the Base class which contains the actual implementation whereas
	 * uniformPath will contain the class name of the most specific subclass, from where it was actually executed.
	 */
	public final String sourcePath;

	/**
	 * Some kind of content to tell whether the test specification has changed. Can be revision number or
	 * hash over the specification or similar.
	 */
	public final String content;

	/** Duration of the execution in seconds. */
	public final Double duration;

	/** The actual execution result state. */
	public final ETestExecutionResult result;

	/**
	 * Optional message given for test failures (normally contains a stack trace).
	 * May be {@code null}.
	 */
	public final String message;

	/** All paths that the test did cover. */
	public final List<PathCoverage> paths = new ArrayList<>();

	@SuppressWarnings("unused") // Moshi might use this (TS-36140)
	public TestInfo() {
		this("", "", "", 0.0, ETestExecutionResult.SKIPPED, "");
	}

	public TestInfo(String uniformPath, String sourcePath, String content, Double duration, ETestExecutionResult result,
					String message) {
		this.uniformPath = uniformPath;
		this.sourcePath = sourcePath;
		this.content = content;
		this.duration = duration;
		this.result = result;
		this.message = message;
	}
}
