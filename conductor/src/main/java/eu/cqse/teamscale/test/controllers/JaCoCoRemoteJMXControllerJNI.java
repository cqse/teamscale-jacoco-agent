package eu.cqse.teamscale.test.controllers;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

/*
 * This class connects to a coverage agent that run in jmx mode.
 *
 * Minimum jacoco version required: 0.6.2
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class JaCoCoRemoteJMXControllerJNI {

    private static final String SERVICE_URL = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";
    private static final String SESSION_ID_SEPARATOR = "!#!";

    private static long startTime;

    public static boolean onTestStart(String address, int port, String testSetName, String testName) {
        try {
            dumpWithId(address, port, "");
            startTime = System.currentTimeMillis();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean onTestFinish(String address, int port, String testSetName, String testName) {
        try {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            String sessionId = testSetName + SESSION_ID_SEPARATOR + testName + SESSION_ID_SEPARATOR + duration;
            dumpWithId(address, port, sessionId);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void dumpWithId(String address, int port, String sessionId) throws IOException {
        JMXServiceURL serviceURL = new JMXServiceURL(String.format(SERVICE_URL, address, port));
        JMXConnector jmxc = JMXConnectorFactory.connect(serviceURL, null);
        final MBeanServerConnection connection = jmxc.getMBeanServerConnection();

        try {
            IAgent agent = MBeanServerInvocationHandler.newProxyInstance(connection, new ObjectName("org.jacoco:type=Runtime"),
                    IAgent.class, false);
            agent.setSessionId(sessionId);
            agent.dump(true);

            // Set session id to empty string after test case to work around dump on exit
            // Otherwise the coverage generated after the last test leads to another
            // duplicate session named like the last test that is written on exit
            agent.setSessionId("");
            jmxc.close();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    public interface IAgent {
        String getVersion();

        String getSessionId();

        void setSessionId(String var1);

        void reset();

        byte[] getExecutionData(boolean var1);

        void dump(boolean var1) throws IOException;
    }

}