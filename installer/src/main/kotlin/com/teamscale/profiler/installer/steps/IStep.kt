package com.teamscale.profiler.installer.steps

import com.teamscale.profiler.installer.*

/**
 * One step in the installation process. Steps are run in a fixed order for installation and in the reverse order during
 * uninstallation.
 */
interface IStep {
	/** Runs this installation step.  */
	@Throws(FatalInstallerError::class)
	fun install(credentials: TeamscaleCredentials)

	/**
	 * Undoes the actions performed in [install]. If part of the uninstallation process
	 * fails, the step continues trying to uninstall as much as possible.
	 *
	 * @param errorReporter all errors that happen during the uninstallation of this step are reported to this.
	 */
	fun uninstall(errorReporter: IUninstallErrorReporter)

	/** Determines whether this step should not be run, e.g., because this step is not applicable to the current OS.  */
	fun shouldRun() = true

	/**
	 * Used to report errors that happen during uninstallation. During uninstalling, we want to remove everything we can
	 * remove, even if some parts of the process fail. Thus, we don't just throw exceptions and abort.
	 */
	interface IUninstallErrorReporter {
		/** Reports the given error to the user.  */
		fun report(e: FatalInstallerError)
	}
}
