package com.teamscale.tia.runlistener

/**
 * Implements simple STDOUT logging for run listeners. We cannot use SLF4J, since the run listeners are executed in the
 * context of the system under test, where we cannot rely on any SLF4J bindings to be present.
 *
 * To enable debug logging, specify `-Dtia.debug` for the JVM containing the run listener.
 */
class RunListenerLogger(private val callerClassName: String) {
	private val debugEnabled: Boolean get() =
		java.lang.Boolean.getBoolean("tia.debug")

	/** Logs a debug message.  */
	fun debug(message: String) {
		if (!debugEnabled) return
		// we log to System.err instead of System.out as some runners will filter System.out, e.g. Maven
		System.err.println("[DEBUG] $callerClassName - $message")
	}

	/** Logs an error message and the stack trace of an optional throwable.  */
	fun error(message: String, throwable: Throwable) {
		System.err.println("[ERROR] $callerClassName - $message")
		throwable.printStackTrace()
	}

	companion object {
		inline fun <reified T> create() = RunListenerLogger(T::class.java.simpleName)
	}
}
