package eu.cqse.test.coverage.recorder.jacoco;

import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This example connects to a coverage agent that run in output mode
 * <code>tcpserver</code> and requests execution data. The collected data is
 * dumped to a local file.
 */
public final class ExecutionDataClient {

    private Socket socket;
    private RemoteControlReader reader;
    private RemoteControlWriter writer;

    public void connectTo(String address, int port) throws IOException {
        socket = new Socket(InetAddress.getByName(address), port);
        writer = new RemoteControlWriter(socket.getOutputStream());
        reader = new RemoteControlReader(socket.getInputStream());
    }

    public void resetExecutionData() throws IOException {
        writer.visitDumpCommand(false, true);
        reader.read();
    }

    public byte[] dumpExecutionData(boolean reset) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ExecutionDataWriter localWriter = new ExecutionDataWriter(outputStream);
        reader.setSessionInfoVisitor(localWriter);
        reader.setExecutionDataVisitor(localWriter);
        writer.visitDumpCommand(true, reset);
        reader.read();
        return outputStream.toByteArray();
    }

    public void closeConnection() throws IOException {
        socket.close();
    }
}