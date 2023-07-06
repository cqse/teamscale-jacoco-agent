package com.teamscale.profiler.installer.steps;

import com.teamscale.profiler.installer.FatalInstallerError;
import com.teamscale.profiler.installer.TeamscaleCredentials;

/**
 * One step in the installation process. Steps must be independent of one
 */
public interface IStep {

	/** Runs this installation step. */
	void install(TeamscaleCredentials credentials) throws FatalInstallerError;

	/** Undoes the actions performed in {@link #install(TeamscaleCredentials)}
	 * @param errorReporter*/
	void uninstall(IUninstallErrorReporter errorReporter);

	/**
	 * Used to report errors that happen during uninstallation. During uninstalling, we want to remove everything we can
	 * remove, even if some parts of the process fail. Thus we don't just throw exceptions and abort.
	 */
	interface IUninstallErrorReporter {

		/** Reports the given error to the user. */
		void report(FatalInstallerError e);
	}

}
