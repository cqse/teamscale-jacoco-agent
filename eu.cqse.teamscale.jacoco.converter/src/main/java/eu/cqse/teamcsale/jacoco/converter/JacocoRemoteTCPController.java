package eu.cqse.teamcsale.jacoco.converter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class JacocoRemoteTCPController implements IJacocoController {

	private Socket socket;
	private RemoteControlReader reader;
	private RemoteControlWriter writer;
	private final String address;
	private final int port;
	private final PublishSubject<ExecutionData> subject = PublishSubject.create();

	public JacocoRemoteTCPController(String address, int port) throws IOException {
		this.address = address;
		this.port = port;
	}

	/**
	 * {@inheritDoc}
	 */
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

	private synchronized boolean isConnected() {
		return socket != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dump(boolean reset) throws IOException {
		// TODO (FS) assert connected
		writer.visitDumpCommand(true, reset);
	}

	@Override
	public synchronized void close() throws IOException {
		if (isConnected()) {
			socket.close();
			socket = null;
		}
	}
}
