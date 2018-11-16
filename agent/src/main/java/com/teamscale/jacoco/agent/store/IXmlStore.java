package com.teamscale.jacoco.agent.store;

import com.teamscale.client.EReportFormat;

/** Stores XML data permanently. */
public interface IXmlStore {

	/** Stores the given XML permanently. */
	void store(String xml, EReportFormat format);

	/** Human-readable description of the store. */
	String describe();

}
