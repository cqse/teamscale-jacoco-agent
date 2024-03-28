package com.teamscale.profiler.installer.utils;

import com.teamscale.profiler.installer.Installer;
import org.assertj.core.api.AbstractAssert;

/** Assertions for {@link com.teamscale.profiler.installer.Installer.UninstallerErrorReporter} */
public class UninstallErrorReporterAssert extends AbstractAssert<UninstallErrorReporterAssert, Installer.UninstallerErrorReporter> {

	protected UninstallErrorReporterAssert(Installer.UninstallerErrorReporter uninstallerErrorReporter,
										   Class<?> selfType) {
		super(uninstallerErrorReporter, selfType);
	}

	/** Asserts that no errors were reported. */
	public void hadNoErrors() {
		if (actual.wereErrorsReported()) {
			failWithMessage("Expected no errors to be reported during the uninstallation, but at least one occurred.");
		}
	}

	/** Asserts at least one error was reported. */
	public void hadErrors() {
		if (!actual.wereErrorsReported()) {
			failWithMessage("Unexpectedly, no errors were reported during the uninstallation.");
		}
	}

	/** Creates an assert for the given reporter. */
	public static UninstallErrorReporterAssert assertThat(Installer.UninstallerErrorReporter reporter) {
		return new UninstallErrorReporterAssert(reporter, UninstallErrorReporterAssert.class);
	}
}
