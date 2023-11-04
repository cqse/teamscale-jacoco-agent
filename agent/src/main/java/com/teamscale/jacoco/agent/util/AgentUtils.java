package com.teamscale.jacoco.agent.util;

import com.teamscale.jacoco.agent.PreMain;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** General utilities for working with the agent. */
public class AgentUtils {

	private static Path mainTempDirectory = null;

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

	public static Path getMainTempDirectory() {
		if (mainTempDirectory == null) {
			try {
				mainTempDirectory = Files.createTempDirectory("teamscale-java-profiler-" + getPID() + "-");
			} catch (IOException e) {
				throw new RuntimeException("Failed to create temporary directory for agent files", e);
			}
		}
		return mainTempDirectory;
	}

	/** Returns the directory that contains the agent or null if it can't be resolved. */
	public static Path getAgentDirectory() {
		try {
			URI jarFileUri = PreMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			// we assume that the dist zip is extracted and the agent jar not moved
			return Paths.get(jarFileUri).getParent().getParent();
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to obtain agent directory. This is a bug, please report it.", e);
		}
	}

}
