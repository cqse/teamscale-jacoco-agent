package com.teamscale.report.util;

/**
 * Minimal logging interface.
 * <p>
 * We use this to work around some strange problems when using log4j from the Teamscale Gradle plugin.
 */
public interface ILogger {

	/** Logs at debug level. */
	void debug(String message);

	/** Logs at info level. */
	void info(String message);

	/** Logs at warning level. */
	void warn(String message);

	/** Logs at warning level. */
	void warn(String message, Throwable throwable);

	/** Logs at error level. */
	void error(Throwable throwable);

	/** Logs at error level. The given {@link Throwable} may be null. */
	void error(String message, Throwable throwable);

	/** Logs at error level. */
	default void error(String message) {
		error(message, null);
	}
}
