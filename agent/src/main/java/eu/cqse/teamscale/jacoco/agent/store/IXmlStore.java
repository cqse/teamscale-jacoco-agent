package eu.cqse.teamscale.jacoco.agent.store;

/** Stores XML data permanently. */
public interface IXmlStore {

	/** Stores the given XML permanently. */
	void store(String xml);

	/** Human-readable description of the store. */
	String describe();

}
