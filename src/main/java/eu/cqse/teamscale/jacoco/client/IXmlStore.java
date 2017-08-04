package eu.cqse.teamscale.jacoco.client;

/** Stores XML data permanently. */
public interface IXmlStore {

	/** Stores the given XML permanently. */
	public void store(String xml);

}
