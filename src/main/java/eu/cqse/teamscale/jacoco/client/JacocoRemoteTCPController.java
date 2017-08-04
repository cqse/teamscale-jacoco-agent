package eu.cqse.teamscale.jacoco.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.jacoco.core.runtime.RemoteControlWriter;

import io.reactivex.Observable;

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
		JacocoReadLoop readLoop = new JacocoReadLoop(socket.getInputStream());
		return readLoop.connect();
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
