package com.teamscale.profiler.installer

/**
 * Fatal error that aborts the installation process due to insufficient permissions for the installer process. E.g.
 * can't create or write files.
 *
 *
 * Throwing this error causes a message to be printed that the user should try running the installer with
 * root/Administrator permissions.
 */
class PermissionError : FatalInstallerError {
	constructor(message: String) : super(message)

	constructor(message: String, throwable: Throwable) : super(message, throwable)
}
