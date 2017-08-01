package eu.cqse.teamscale.jacoco.converter;

import java.io.IOException;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;

import io.reactivex.Observable;

/** Remote-controls a JaCoCo instance. */
public interface IJacocoController extends AutoCloseable {

	/**
	 * Hot observable stream of dump data. Does not operate on any scheduler by
	 * default.
	 */
	Observable<Dump> connect() throws IOException;

	/**
	 * Requests a dump, optionally with a reset of the data. Received dumps will be
	 * sent through the observable returned by {@link #connect()}.
	 * 
	 * May not be called from more than one thread at a time.
	 */
	void dump(boolean reset) throws IOException;

	/** Closes the connection to JaCoCo. */
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