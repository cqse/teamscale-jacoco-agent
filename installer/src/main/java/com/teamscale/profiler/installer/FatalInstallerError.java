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
	 * Prints this error to stderr. The stack trace of this exception is suppressed, but stack traces of any cause are
	 * printed.
	 * <p>
	 * we suppress stack traces of FatalInstallerErrors since these are errors we handled explicitly and the message
	 * itself is supposed to be clear.
	 */
	public void printToStderr() {
		System.err.println(getMessage());
		if (getCause() != null) {
			getCause().printStackTrace(System.err);
		}
	}

}
