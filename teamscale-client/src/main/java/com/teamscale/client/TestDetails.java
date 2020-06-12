package com.teamscale.client;

import java.util.Objects;

/**
 * Contains details about a test.
 */
public class TestDetails {

	/** Unique name of the test case by using a path like hierarchical description, which can be shown in the UI. */
	public final String uniformPath;

	/**
	 * Path to the source of the method. Will be equal to uniformPath in most cases, but e.g. @Test methods in a base
	 * class will have the sourcePath pointing to the base class which contains the actual implementation whereas
	 * uniformPath will contain the the class name of the most specific subclass, from where it was actually executed.
	 */
	public final String sourcePath;

	/**
	 * Some kind of content to tell whether the test specification has changed. Can be revision number or hash over the
	 * specification or similar. You can include e.g. a hash of each test's test data so that whenever the test data
	 * changes, the corresponding test is re-run.
	 */
	public final String content;

	public TestDetails(String uniformPath, String sourcePath, String content) {
		this.uniformPath = uniformPath;
		this.sourcePath = sourcePath;
		this.content = content;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TestDetails that = (TestDetails) o;
		return Objects.equals(uniformPath, that.uniformPath) &&
				Objects.equals(sourcePath, that.sourcePath) &&
				Objects.equals(content, that.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uniformPath, sourcePath, content);
	}
}