package com.teamscale.profiler.installer.utils

import com.teamscale.profiler.installer.Installer.UninstallerErrorReporter
import org.assertj.core.api.AbstractAssert

/** Assertions for [com.teamscale.profiler.installer.Installer.UninstallerErrorReporter]  */
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
