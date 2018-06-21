/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.dump;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;

/** All data received in one dump. */
public class Dump {

	/** The session info. */
	public final SessionInfo info;

	/** The execution data store. */
	public final ExecutionDataStore store;

	/** Constructor. */
	public Dump(SessionInfo info, ExecutionDataStore store) {
		this.info = info;
		this.store = store;
	}

}
