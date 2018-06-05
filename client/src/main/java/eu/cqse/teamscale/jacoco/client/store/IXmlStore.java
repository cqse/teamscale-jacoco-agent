package eu.cqse.teamscale.jacoco.client.store;

/** Stores XML data permanently. */
public interface IXmlStore {

	/** Stores the given XML permanently. */
	public void store(String xml);

	/** Human-readable description of the store. */
	public String describe();

}
