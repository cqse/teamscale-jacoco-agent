package com.teamscale.profiler.installer

/** Fatal error that aborts the installation process.  */
open class FatalInstallerError : Exception {
	constructor(message: String) : super(message)

	constructor(message: String, throwable: Throwable) : super(message, throwable)

	/**
	 * Prints this error to stderr. All stack traces of this exception are suppressed since these are errors we handled
	 * explicitly and the message itself is supposed to be clear.
	 * If the error has a cause, its message is printed.
	 */
	fun printToStderr() {
		System.err.println(message)
		cause?.printStackTrace()
	}
}
