package eu.cqse.teamscale.jacoco.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.RemoteControlReader;

import eu.cqse.teamscale.jacoco.converter.IJacocoController.Dump;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Manages a background read loop on an IO thread that reads from the JaCoCo
 * socket.
 */
public class JacocoReadLoop {

	/** Dummy session ID to use when no session is supplied. */
	private static final String DUMMY_SESSION_ID = "dummysessionid";

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** Input stream from the socket. */
	private final InputStream socketInputStream;

	/** Stores the execution data received from the socket. */
	private ExecutionDataStore store = new ExecutionDataStore();

	/**
	 * Constructor.
	 */
	public JacocoReadLoop(InputStream socketInputStream) {
		this.socketInputStream = socketInputStream;
	}

	/**
	 * Connects to the socket and starts a background read loop. Dumps read from the
	 * input stream will be passed through the returned observable.
	 * 
	 * @throws IOException
	 *             if the initial socket connection fails.
	 */
	public Observable<Dump> connect() throws IOException {
		RemoteControlReader reader = new RemoteControlReader(socketInputStream);
		reader.setExecutionDataVisitor(store::put);

		Observable<SessionInfo> sessionInfoObservable = Observable.<SessionInfo>create(emitter -> {
			reader.setSessionInfoVisitor(info -> {
				logger.info("Received session info");
				emitter.onNext(info);
			});
			// ensure we have a default value in case we ever receive execution data but no
			// session data
		}).startWith(new SessionInfo(DUMMY_SESSION_ID, new Date().getTime(), 123l));

		Observable<ExecutionDataStore> executionDataObservable = Observable.<ExecutionDataStore>create(emitter -> {
			try {
				logger.info("Starting to read from socket");
				while (reader.read()) {
					logger.debug("Received data");
					emitter.onNext(store);
					store = new ExecutionDataStore();
					logger.debug("Waiting for next read");
				}
				logger.info("Socket input ended gracefully");
				emitter.onComplete();
			} catch (IOException e) {
				emitter.onError(e);
			}
		});

		return executionDataObservable
				// perform read loop on IO thread but don't block the IO thread while
				// parsing the coverage data
				.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation())
				.withLatestFrom(sessionInfoObservable, Dump::new).doOnNext(dump -> {
					if (dump.info.getId().equals(DUMMY_SESSION_ID)) {
						logger.warn("Got execution data without first receiving a session. Using the dummy session.");
					}
				});
	};
}
