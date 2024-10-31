package com.teamscale.report.util

/**
 * Minimal logging interface.
 *
 * We use this to work around some strange problems when using log4j from the Teamscale Gradle plugin.
 */
interface ILogger {

	/** Logs at debug level. */
	fun debug(message: String)

	/** Logs at info level. */
	fun info(message: String)

	/** Logs at warning level. */
	fun warn(message: String)

	/** Logs at warning level. The given [Throwable] may be null. */
	fun warn(message: String, throwable: Throwable?)

	/** Logs at error level. */
	fun error(throwable: Throwable)

	/** Logs at error level. The given [Throwable] may be null. */
	fun error(message: String, throwable: Throwable? = null)
}
