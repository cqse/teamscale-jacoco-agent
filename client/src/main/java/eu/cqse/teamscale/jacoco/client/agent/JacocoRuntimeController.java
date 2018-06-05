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
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

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
			reader.setExecutionDataVisitor(store::put);

			SessionInfoVisitor sessionInfoVisitor = new SessionInfoVisitor();
			reader.setSessionInfoVisitor(sessionInfoVisitor);

			reader.read();
			return new Dump(store, sessionInfoVisitor.sessionInfo);
		} catch (IOException e) {
			throw new DumpException("should never happen for the ByteArrayInputStream", e);
		}
	}

	/**
	 * Receives and stores a {@link SessionInfo}. Has a fallback dummy session in
	 * case nothing is received.
	 */
	private static class SessionInfoVisitor implements ISessionInfoVisitor {

		/** The received session info or a dummy. */
		public SessionInfo sessionInfo = new SessionInfo("dummysession", System.currentTimeMillis(),
				System.currentTimeMillis());

		/** {@inheritDoc} */
		@Override
		public void visitSessionInfo(SessionInfo info) {
			this.sessionInfo = info;
		}

	}

}
