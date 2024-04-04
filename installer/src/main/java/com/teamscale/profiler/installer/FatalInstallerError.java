package com.teamscale.profiler.installer;

/** Fatal error that aborts the installation process. */
public class FatalInstallerError extends Exception {

	public FatalInstallerError(String message) {
		super(message);
	}

	public FatalInstallerError(String message, Throwable throwable) {
		super(message, throwable);
	}

	/**
	 * Prints this error to stderr. All stack traces of this exception are suppressed since these are errors we handled
	 * explicitly and the message itself is supposed to be clear.
	 * If the error has a cause, its message is printed.
	 */
	public void printToStderr() {
		System.err.println(getMessage());
		if (getCause() != null) {
			System.err.println(getCause().getMessage());
		}
	}

}
