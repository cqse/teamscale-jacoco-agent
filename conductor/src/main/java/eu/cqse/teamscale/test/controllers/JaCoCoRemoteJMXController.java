package eu.cqse.teamscale.test.controllers;

import static eu.cqse.teamscale.jacoco.report.XMLCoverageWriter.SESSION_ID_SEPARATOR;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jacoco.agent.rt.IAgent;

/*
 * This class connects to a coverage agent that run in jmx mode.
 *
 * Minimum jacoco version required: 0.6.2
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class JaCoCoRemoteJMXController implements IJaCoCoController {

	private static final String SERVICE_URL = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";

	private String address;
	private int port;

	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private long startTime;
	private IAgent agent;
	private JMXConnector jmxc;

	public JaCoCoRemoteJMXController(String address, int port) throws IOException {
		this.address = address;
		this.port = port;
	}

	public void connect() throws IOException {
		JMXServiceURL serviceURL = new JMXServiceURL(String.format(SERVICE_URL, address, port));
		jmxc = JMXConnectorFactory.connect(serviceURL, null);
		final MBeanServerConnection connection = jmxc.getMBeanServerConnection();

		try {
			agent = MBeanServerInvocationHandler.newProxyInstance(connection, new ObjectName("org.jacoco:type=Runtime"),
					IAgent.class, false);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	@Override
	public void onTestStart(String className, String testName) {
		lock.writeLock().lock();
		// Dump coverage between tests
		try {
			dumpWithSessionId("");
		} catch (IOException e) {
			e.printStackTrace();
			lock.writeLock().unlock();
		}
		startTime = System.currentTimeMillis();
	}

	@Override
	public void onTestFinish(String className, String testName) {
		try {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			dumpWithSessionId(className + SESSION_ID_SEPARATOR + testName + SESSION_ID_SEPARATOR + duration);
		} catch (Exception e) {
			e.printStackTrace();
			throw new JacocoControllerError(e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void dumpWithSessionId(String sessionId) throws IOException {
		agent.setSessionId(sessionId);
		agent.dump(true);

		// Set session id to empty string after test case to work around dump on exit
		// Otherwise the coverage generated after the last test leads to another
		// duplicate session named like the last test that is written on exit
		agent.setSessionId("");
	}

	private boolean isConnected() {
		try {
			return jmxc != null && jmxc.getConnectionId() != null;
		} catch (IOException e) {
			return false;
		}
	}

	public void close() throws IOException {
		jmxc.close();
	}
}
