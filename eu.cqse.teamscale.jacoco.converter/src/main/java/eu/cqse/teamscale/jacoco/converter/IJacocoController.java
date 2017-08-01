package eu.cqse.teamscale.jacoco.converter;

import java.io.IOException;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;

import io.reactivex.Observable;

public interface IJacocoController extends AutoCloseable {

	Observable<Dump> connect() throws IOException;

	void dump(boolean reset) throws IOException;

	@Override
	void close() throws IOException;

	/** All data received in one dump. */
	public static class Dump {

		/** The session info. */
		public final SessionInfo info;

		/** The execution data store. */
		public final ExecutionDataStore store;

		/**
		 * Constructor.
		 */
		public Dump(SessionInfo info, ExecutionDataStore store) {
			this.info = info;
			this.store = store;
		}

	}

}