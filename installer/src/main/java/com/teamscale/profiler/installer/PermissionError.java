package com.teamscale.profiler.installer;

/**
 * Fatal error that aborts the installation process due to insufficient permissions for the installer process. E.g.
 * can't create or write files.
 * <p>
 * Throwing this error causes a message to be printed that the user should try running the installer with
 * root/Administrator permissions.
 */
public class PermissionError extends FatalInstallerError {

	public PermissionError(String message) {
		super(message);
	}

	public PermissionError(String message, Throwable throwable) {
		super(message, throwable);
	}

}
