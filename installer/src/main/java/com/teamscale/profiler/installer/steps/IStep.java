package com.teamscale.profiler.installer.steps;

import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.TeamscaleCredentials;

/**
 * One step in the installation process. Steps are run in a fixed order for installation and in the reverse order during
 * uninstallation.
 */
public interface IStep {

	/** Runs this installation step. */
	void install(TeamscaleCredentials credentials) throws FatalInstallerError;

	/**
	 * Undoes the actions performed in {@link #install(TeamscaleCredentials)}. If part of the uninstallation process
	 * fails, the step continues trying to uninstall as much as possible.
	 *
	 * @param errorReporter all errors that happen during the uninstallation of this step are reported to this.
	 */
	void uninstall(IUninstallErrorReporter errorReporter);

	default boolean shouldRun() {
		return true;
	}

	/**
	 * Used to report errors that happen during uninstallation. During uninstalling, we want to remove everything we can
	 * remove, even if some parts of the process fail. Thus we don't just throw exceptions and abort.
	 */
	interface IUninstallErrorReporter {

		/** Reports the given error to the user. */
		void report(FatalInstallerError e);
	}

}
