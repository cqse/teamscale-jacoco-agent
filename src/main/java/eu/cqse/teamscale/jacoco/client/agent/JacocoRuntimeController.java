/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

import org.jacoco.agent.rt.RT;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;

import eu.cqse.teamscale.jacoco.client.watch.IJacocoController.Dump;

/**
 * Wrapper around JaCoCo's {@link RT} runtime interface.
 * 
 * Can be used if the calling code is run in the same JVM as the agent is
 * attached to.
 */
public class JacocoRuntimeController {

	/** Dumps execution data and resets it. */
	public Dump dumpAndReset() throws IllegalStateException, IOException {
		// TODO (FS) exception handling
		byte[] binaryData = RT.getAgent().getExecutionData(true);

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(binaryData)) {
			ExecutionDataReader reader = new ExecutionDataReader(inputStream);
			ExecutionDataStore store = new ExecutionDataStore();
			SessionInfo sessionInfo = new SessionInfo("dummysession", new Date().getTime(), 123l);
			reader.setExecutionDataVisitor(store::put);
			reader.read();
			return new Dump(store, sessionInfo);
		}
	}

}
