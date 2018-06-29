package eu.cqse.teamscale.jacoco.util;

/**
 * Minimal logging interface.
 * <p>
 * We use this to work around some strange problems when using log4j from the Teamscale Gradle plugin.
 */
public interface ILogger {

	/** Logs at debug level. */
	void debug(String debugLog);

	/** Logs at warning level. */
	void warn(String s, Throwable e);

	/** Logs at error level. */
	void error(Throwable e);
}
