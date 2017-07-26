package eu.cqse.teamscale.test.controllers;

import eu.cqse.teamscale.jacoco.report.XMLCoverageWriter;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

@SuppressWarnings({"WeakerAccess", "unused"})
public class JaCoCoRemoteTCPController implements IJaCoCoController {

    private Socket socket;
    private RemoteControlReader reader;
    private RemoteControlWriter writer;
    private boolean testStarted;
    private String address;
    private int port;
    private final ExecutionDataWriter localWriter;
    private final BufferedOutputStream outputStream;

    public JaCoCoRemoteTCPController(String address, int port, File execFile) throws IOException {
        this.address = address;
        this.port = port;
        outputStream = new BufferedOutputStream(new FileOutputStream(execFile));
        localWriter = new ExecutionDataWriter(outputStream);
    }

    public synchronized void connect() throws IOException {
        socket = new Socket(InetAddress.getByName(address), port);
        writer = new RemoteControlWriter(socket.getOutputStream());
        reader = new RemoteControlReader(socket.getInputStream());
        reader.setSessionInfoVisitor(new ISessionInfoVisitor() {
            @Override
            public void visitSessionInfo(SessionInfo info) {
            }
        });
        reader.setExecutionDataVisitor(localWriter);
    }

    @Override
    public synchronized void onTestStart(String testSetName, String testName) {
        if (testStarted) {
            throw new JacocoControllerError("Looks like several tests executed in parallel in the same JVM, thus coverage per test can't be recorded correctly.");
        }
        if (isConnected()) {
            try {
                writer.visitDumpCommand(false, true);
                reader.read();
            } catch (IOException e) {
                throw new JacocoControllerError(e);
            }
        }
    }

    public synchronized boolean isConnected() {
        return socket != null;
    }

    @Override
    public synchronized void onTestFinish(String testSetName, String testName) {
        testStarted = false;
        try {
            String sessionId = testSetName + XMLCoverageWriter.SESSION_ID_SEPARATOR + testName;
            localWriter.visitSessionInfo(new SessionInfo(sessionId, 0, System.currentTimeMillis()));
            writer.visitDumpCommand(true, true);
            reader.read();
            localWriter.flush();
        } catch (IOException e) {
            throw new JacocoControllerError(e);
        }
    }

    public synchronized void disconnect() throws IOException {
        if (isConnected()) {
            socket.close();
            socket = null;
        }
    }

    public synchronized void close() throws IOException {
        try {
            disconnect();
        } catch (IOException ignore) {
        }
        outputStream.close();
    }
}
