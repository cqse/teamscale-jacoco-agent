package eu.cqse.teamscale.jacoco.converter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.conqat.lib.commons.assertion.CCSMAssert;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
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
	private final PublishSubject<ExecutionData> subject = PublishSubject.create();

	/** Constructor. */
	public JacocoRemoteTCPController(String address, int port) throws IOException {
		this.address = address;
		this.port = port;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized Observable<ExecutionData> connect() throws IOException {
		socket = new Socket(InetAddress.getByName(address), port);
		writer = new RemoteControlWriter(socket.getOutputStream());
		reader = new RemoteControlReader(socket.getInputStream());
		reader.setSessionInfoVisitor(info -> {
			// ignored
		});
		reader.setExecutionDataVisitor(subject::onNext);
		return subject.subscribeOn(Schedulers.io());
	}

	/** Returns <code>true</code> if the socket is open. */
	private synchronized boolean isConnected() {
		return socket != null;
	}

	/** {@inheritDoc} */
	@Override
	public void dump(boolean reset) throws IOException {
		CCSMAssert.isTrue(isConnected(), "You must connect before you can start issuing dump commands");
		writer.visitDumpCommand(true, reset);
		// must read afterwards or data will not be received
		reader.read();
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
