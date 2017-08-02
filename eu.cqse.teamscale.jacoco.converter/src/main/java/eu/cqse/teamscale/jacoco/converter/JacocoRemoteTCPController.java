package eu.cqse.teamscale.jacoco.converter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/** Connects to JaCoCo via a TCP port. */
public class JacocoRemoteTCPController implements IJacocoController {

	/** The communication socket. */
	private Socket socket;

	/** The command writer. */
	private RemoteControlWriter writer;

	/** The address of JaCoCo. */
	private final String address;

	/** The port of JaCoCo. */
	private final int port;

	/** Temporarily stores execution data. */
	private ExecutionDataStore store = new ExecutionDataStore();

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** Constructor. */
	public JacocoRemoteTCPController(String address, int port) throws IOException {
		this.address = address;
		this.port = port;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized Observable<Dump> connect() throws IOException {
		socket = new Socket(InetAddress.getByName(address), port);
		writer = new RemoteControlWriter(socket.getOutputStream());

		RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
		reader.setExecutionDataVisitor(store::put);
		Observable<SessionInfo> sessionInfoObservable = Observable.<SessionInfo>create(emitter -> {
			reader.setSessionInfoVisitor(info -> {
				logger.info("Received session info");
				emitter.onNext(info);
			});
		}).startWith(new SessionInfo("dummysessionid", new Date().getTime(), 123l));

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
				.withLatestFrom(sessionInfoObservable, Dump::new);
	}

	/** Returns <code>true</code> if the socket is open. */
	private synchronized boolean isConnected() {
		return socket != null;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void dump(boolean reset) throws IOException {
		CCSMAssert.isTrue(isConnected(), "You must connect before you can start issuing dump commands");
		writer.visitDumpCommand(true, reset);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void close() throws IOException {
		if (isConnected()) {
			socket.close();
			socket = null;
			logger.info("Socket closed");
		}
	}
}
