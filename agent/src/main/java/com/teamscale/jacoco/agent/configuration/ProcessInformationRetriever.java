package com.teamscale.jacoco.agent.configuration;

import com.teamscale.client.ProcessInformation;
import com.teamscale.report.util.ILogger;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The ProcessInformationRetriever class is responsible for retrieving process information such as the host name and
 * process ID.
 */
public class ProcessInformationRetriever {

	private final ILogger logger;

	public ProcessInformationRetriever(ILogger logger) {
		this.logger = logger;
	}

	/**
	 * Retrieves the process information, including the host name and process ID.
	 */
	public ProcessInformation getProcessInformation() {
		String hostName = getHostName();
		String processId = getPID();
		return new ProcessInformation(hostName, processId, System.currentTimeMillis());
	}

	/**
	 * Retrieves the host name of the local machine.
	 */
	private String getHostName() {
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			return inetAddress.getHostName();
		} catch (UnknownHostException e) {
			logger.error("Failed to determine hostname!", e);
			return "";
		}
	}


	/**
	 * Returns a string that <i>probably</i> contains the PID.
	 * <p>
	 * On Java 9 there is an API to get the PID. But since we support Java 8, we may fall back to an undocumented API
	 * that at least contains the PID in most JVMs.
	 * <p>
	 * See <a href="https://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id">This
	 * StackOverflow question</a>
	 */
	public static String getPID() {
		try {
			Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
			Object processHandle = processHandleClass.getMethod("current").invoke(null);
			Long pid = (Long) processHandleClass.getMethod("pid").invoke(processHandle);
			return pid.toString();
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
				 InvocationTargetException e) {
			return ManagementFactory.getRuntimeMXBean().getName();
		}
	}
}
