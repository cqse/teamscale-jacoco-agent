package eu.cqse.teamscale.jacoco.agent.testimpact;

/**
 * Contains details about a test.
 * This is mainly used to store the mapping from internalId to externalId and vice versa.
 */
public class TestDetails {

	/** Unique name of the test case which can be used by an external build system to find and execute the test. */
	public final String externalId;

	/** Unique name of the test case by using a path like hierarchical description, which can be shown in the UI. */
	public final String internalId;

	/**
	 * Path to the source ot the method. Will be equal to uniformPath in most cases, but e.g. @Test methods in a Base
	 * class will have the sourcePath pointing to the Base class which contains the actual implementation whereas
	 * uniformPath will contain the the class name of the most specific subclass, from where it was actually executed.
	 */
	public final String sourcePath;

	/** The name of the test case in a human readable form. */
	public final String displayName;

	/**
	 * Some kind of content to tell whether the test specification has changed. Can be revision number or
	 * hash over the specification or similar.
	 */
	public final String content;

	/** Constructor. */
	public TestDetails(String externalId, String internalId, String sourcePath, String displayName, String content) {
		this.externalId = externalId;
		this.internalId = internalId;
		this.sourcePath = sourcePath;
		this.displayName = displayName;
		this.content = content;
	}
}