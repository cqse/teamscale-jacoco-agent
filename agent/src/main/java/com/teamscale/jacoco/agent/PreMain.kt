package com.teamscale.jacoco.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/** Container class for the premain entry point for the agent. */
public class PreMain {

	/**
	 * Entry point for the agent, called by the JVM.
	 * The only purpose of this method is to enforce classloader isolation of
	 * the agent from the rest of the application by running the actual agent
	 * in a custom classloader that only has access to the agent's Jar file.
	 * <p>
	 * We do this by temporarily changing the thread's context classloader to
	 * an {@link URLClassLoader} without a parent, starting the agent and then
	 * resetting the context classloader.
	 */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		URL agentJarUrl = PreMain.class.getProtectionDomain().getCodeSource().getLocation();
		URLClassLoader classloader = new URLClassLoader(new URL[]{agentJarUrl}, null);
		Thread.currentThread().setContextClassLoader(classloader);

		// we must use reflection to access the agent's methods because we don't share
		// the agent's classloader
		String agentName = PreMain.class.getPackage().getName() + ".AgentBase";
		Class<?> agentClass = classloader.loadClass(agentName);
		Method method = agentClass.getMethod("premain", String.class, Instrumentation.class);
		method.invoke(null, options, instrumentation);

		Thread.currentThread().setContextClassLoader(contextClassLoader);
	}
}
