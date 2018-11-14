package eu.cqse.teamscale.report.testwise.model;

import eu.cqse.teamscale.report.testwise.model.builder.TestCoverageBuilder;

import java.util.ArrayList;
import java.util.List;

/** Generic container of all information about a specific test as written to the report. */
public class TestInfo {

	/** Unique name of the test case by using a path like hierarchical description, which can be shown in the UI. */
	private final String uniformPath;

	/**
	 * Path to the source of the method. Will be equal to uniformPath in most cases, but e.g. @Test methods in a Base
	 * class will have the sourcePath pointing to the Base class which contains the actual implementation whereas
	 * uniformPath will contain the the class name of the most specific subclass, from where it was actually executed.
	 */
	public final String sourcePath;

	/**
	 * Some kind of content to tell whether the test specification has changed. Can be revision number or
	 * hash over the specification or similar.
	 */
	public final String content;

	/** Duration of the execution in seconds. */
	private final Double duration;

	/** The actual execution result state. */
	private final ETestExecutionResult result;

	/**
	 * Optional message given for test failures (normally contains a stack trace).
	 * May be {@code null}.
	 */
	private final String message;

	public final List<PathCoverage> paths = new ArrayList<>();

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
