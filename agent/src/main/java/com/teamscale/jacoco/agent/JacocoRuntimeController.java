/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent;

import com.teamscale.report.jacoco.dump.Dump;
import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Wrapper around JaCoCo's {@link RT} runtime interface.
 * <p>
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

	/** JaCoCo's {@link RT} agent instance */
	private final IAgent agent;

	/** Constructor. */
	public JacocoRuntimeController(IAgent agent) {
		this.agent = agent;
	}

	/**
	 * Dumps execution data and resets it.
	 *
	 * @throws DumpException if dumping fails. This should never happen in real life. Dumping
	 *                       should simply be retried later if this ever happens.
	 */
	public Dump dumpAndReset() throws DumpException {
		byte[] binaryData = agent.getExecutionData(true);

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(binaryData)) {
			ExecutionDataReader reader = new ExecutionDataReader(inputStream);

			ExecutionDataStore store = new ExecutionDataStore();
			reader.setExecutionDataVisitor(store::put);

			SessionInfoVisitor sessionInfoVisitor = new SessionInfoVisitor();
			reader.setSessionInfoVisitor(sessionInfoVisitor);

			reader.read();
			return new Dump(sessionInfoVisitor.sessionInfo, store);
		} catch (IOException e) {
			throw new DumpException("should never happen for the ByteArrayInputStream", e);
		}
	}

	/**
	 * Dumps execution data to a file and resets it.
	 *
	 * @throws DumpException if dumping fails. This should never happen in real life. Dumping
	 *                       should simply be retried later if this ever happens.
	 */
	public void dump() throws DumpException {
		try {
			agent.dump(true);
		} catch (IOException e) {
			throw new DumpException(e.getMessage(), e);
		}
	}

	/** Resets already collected coverage. */
	public void reset() {
		agent.reset();
	}

	/**
	 * Sets the current sessionId of the agent that can be used to identify
	 * which coverage is recorded from now on.
	 */
	public void setSessionId(String sessionId) {
		agent.setSessionId(sessionId);
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
