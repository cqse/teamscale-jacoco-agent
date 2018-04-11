/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jacoco.agent.rt.IAgent;
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

	/** Indicates a failed dump. */
	public static class DumpException extends Exception {

		/** Serialization ID. */
		private static final long serialVersionUID = 1L;

		/** Constructor. */
		public DumpException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	/**
	 * Dumps execution data and resets it.
	 * 
	 * @throws DumpException
	 *             if dumping fails. This should never happen in real life. Dumping
	 *             should simply be retried later if this ever happens.
	 */
	public Dump dumpAndReset() throws DumpException {
		IAgent agent;
		try {
			agent = RT.getAgent();
		} catch (IllegalStateException e) {
			throw new DumpException("JaCoCo agent not yet initialized", e);
		}

		byte[] binaryData = agent.getExecutionData(true);

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(binaryData)) {
			ExecutionDataReader reader = new ExecutionDataReader(inputStream);
			ExecutionDataStore store = new ExecutionDataStore();
			SessionInfo sessionInfo = new SessionInfo("dummysession", System.currentTimeMillis(),
					System.currentTimeMillis());
			reader.setExecutionDataVisitor(store::put);
			reader.read();
			return new Dump(store, sessionInfo);
		} catch (IOException e) {
			throw new DumpException("should never happen for the ByteArrayInputStream", e);
		}
	}

}
