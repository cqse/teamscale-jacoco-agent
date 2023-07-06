package com.teamscale.profiler.installer;

/** Fatal error that aborts the installation process. */
public class FatalInstallerError extends Exception {

	public FatalInstallerError(String message) {
		super(message);
	}

	public FatalInstallerError(String message, Throwable throwable) {
		super(message, throwable);
	}

	public void printToStderr() {
		System.err.println(getMessage());
		if (getCause() != null) {
			// we suppress stack traces of FatalInstallerErrors since these are errors we handled explicitly
			// and the message itself is supposed to be clear
			getCause().printStackTrace(System.err);
		}
	}

}
