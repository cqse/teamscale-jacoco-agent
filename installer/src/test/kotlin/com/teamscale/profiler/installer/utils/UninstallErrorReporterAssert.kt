package com.teamscale.profiler.installer.utils

import com.teamscale.profiler.installer.Installer.UninstallerErrorReporter
import org.assertj.core.api.AbstractAssert

/**
 * Assertion class for verifying the behavior of the `UninstallerErrorReporter` during uninstallation
 * processes.
 * Provides utility methods to assert whether errors were reported or not.
 *
 * @param uninstallerErrorReporter The `UninstallerErrorReporter` instance to run assertions on.
 * @param selfType Class type of the custom assertion implementation.
 */
class UninstallErrorReporterAssert(
	uninstallerErrorReporter: UninstallerErrorReporter,
	selfType: Class<*>
) : AbstractAssert<UninstallErrorReporterAssert, UninstallerErrorReporter>(uninstallerErrorReporter, selfType) {

	/** Asserts that no errors were reported.  */
	fun hadNoErrors() {
		if (!actual.wereErrorsReported()) return
		failWithMessage("Expected no errors to be reported during the uninstallation, but at least one occurred.")
	}

	/** Asserts at least one error was reported.  */
	fun hadErrors() {
		if (actual.wereErrorsReported()) return
		failWithMessage("Unexpectedly, no errors were reported during the uninstallation.")
	}

	companion object {
		/** Creates an assertion for the given reporter.  */
		fun assertThat(reporter: UninstallerErrorReporter) =
			UninstallErrorReporterAssert(reporter, UninstallErrorReporterAssert::class.java)
	}
}
