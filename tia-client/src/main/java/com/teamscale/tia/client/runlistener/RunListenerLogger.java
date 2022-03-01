package com.teamscale.tia.client.runlistener;

/**
 * Implements simple STDOUT logging for run listeners. We cannot use SLF4J, since the run listeners are executed in the
 * context of the system under test, where we cannot rely on any SLF4J bindings to be present.
 * <p>
 * To enable debug logging, specify {@code -DTIA_DEBUG} for the JVM containing the run listener.
 */
public class RunListenerLogger {

	private static final boolean DEBUG_ENABLED = Boolean.getBoolean("TIA_DEBUG");

	private final String callerClassName;

	public RunListenerLogger(Class<?> callerClass) {
		callerClassName = callerClass.getName();
	}

	public void debug(String message) {
		if (!DEBUG_ENABLED) {
			return;
		}
		// we log to System.err instead of System.out as some runners will filter System.out, e.g. Maven
		System.err.println("[DEBUG] " + callerClassName + " - " + message);
	}

	public void error(String message, Throwable throwable) {
		System.err.println("[ERROR] " + callerClassName + " - " + message);
		throwable.printStackTrace();
	}

}
