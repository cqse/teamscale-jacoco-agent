package eu.cqse.teamscale.jacoco.agent.store;

import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService;

/** Stores XML data permanently. */
public interface IXmlStore {

	/** Stores the given XML permanently. */
	void store(String xml, ITeamscaleService.EReportFormat format);

	/** Human-readable description of the store. */
	String describe();

}
