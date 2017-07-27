package eu.cqse.teamscale.jacoco.converter;

/** Stores XML data permanently. */
public interface IXmlStore {

	/** Stores the given XML permanently. */
	public void store(String xml);

}
