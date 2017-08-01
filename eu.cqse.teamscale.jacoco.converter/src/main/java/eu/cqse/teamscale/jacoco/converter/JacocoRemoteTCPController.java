package eu.cqse.teamscale.jacoco.converter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/** Connects to JaCoCo via a TCP port. */
public class JacocoRemoteTCPController implements IJacocoController {

	/** The communication socket. */
	private Socket socket;

	/** The data reader. */
	private RemoteControlReader reader;

	/** The command writer. */
	private RemoteControlWriter writer;

	/** The address of JaCoCo. */
	private final String address;

	/** The port of JaCoCo. */
	private final int port;

	/** Subject used to publish dumped execution data. */
	private final PublishSubject<Dump> subject = PublishSubject.create();

	/** Temporarily stores execution data. */
	private ExecutionDataStore store = new ExecutionDataStore();

	/**
	 * The last received session info. Setting a dummy default value prevents NPEs
	 * in case we should ever not receive a session on dump.
	 */
	private SessionInfo lastReceivedSession = new SessionInfo("dummyid", new Date().getTime(), 123l);

	/** Constructor. */
	public JacocoRemoteTCPController(String address, int port) throws IOException {
		this.address = address;
		this.port = port;
	}

	/** {@inheritDoc} */
	@Override
	// TODO (FS) returned observable should be observed on background thread so we
	// don't block the reader thread
	// TODO (FS) thus, observe on single later on for synchronization of dumps
	// TODO (FS) clean up background read thread etc with observables
	public synchronized Observable<Dump> connect() throws IOException {
		socket = new Socket(InetAddress.getByName(address), port);
		writer = new RemoteControlWriter(socket.getOutputStream());
		reader = new RemoteControlReader(socket.getInputStream());
		reader.setSessionInfoVisitor(info -> lastReceivedSession = info);
		reader.setExecutionDataVisitor(this::storeExecutionData);

		new Thread(() -> {
			Logger logger = LogManager.getLogger(this);
			try {
				logger.info("starting read loop");
				while (reader.read()) {
					logger.info("wait for next read");
					subject.onNext(new Dump(lastReceivedSession, store));
					store = new ExecutionDataStore();
				}
			} catch (IOException e) {
				logger.error("failed to read", e);
			}
			logger.info("stream ended");
		}).start();

		return subject;
	}

	/** Synchronizes storing execution data. */
	public synchronized void storeExecutionData(ExecutionData data) {
		store.put(data);
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
		}
	}
}
