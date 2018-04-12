package eu.cqse.teamscale.jacoco.client.store;

/** Stores XML data permanently. */
public interface IXmlStore {

	/** Stores the given XML permanently. */
	public void store(String xml);

}
