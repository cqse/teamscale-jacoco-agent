package com.teamscale.profiler.installer;

/** Fatal error that aborts the installation process. */
public class FatalInstallerError extends Exception {

	public FatalInstallerError(String message) {
		super(message);
	}

	public FatalInstallerError(String message, Throwable throwable) {
		super(message, throwable);
	}

}
