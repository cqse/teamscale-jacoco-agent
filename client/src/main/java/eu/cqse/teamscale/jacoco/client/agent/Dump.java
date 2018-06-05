/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.agent;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;

/** All data received in one dump. */
public class Dump {

	/** The session info. */
	public final SessionInfo info;

	/** The execution data store. */
	public final ExecutionDataStore store;

	/** Constructor. */
	public Dump(ExecutionDataStore store, SessionInfo info) {
		this.info = info;
		this.store = store;
	}

}