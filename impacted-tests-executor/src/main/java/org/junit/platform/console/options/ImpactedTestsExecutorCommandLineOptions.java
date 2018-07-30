package org.junit.platform.console.options;

import eu.cqse.teamscale.client.CommitDescriptor;

import java.nio.file.Path;
import java.util.Optional;

/** Holds the actual values of the options configured via command line parameters. */
public class ImpactedTestsExecutorCommandLineOptions {

	/** Connection details of the Teamscale server. */
	public final Server server = new Server();

	/** The partition to upload test details to and get impacted tests from. */
	public String partition;

	/** Executes all tests, not only impacted ones if set. */
	public boolean runAllTests;

	/** The baseline commit used for TIA. */
    public CommitDescriptor baseline;

	/** The end commit used for TIA and for uploading the coverage. */
	public CommitDescriptor endCommit;

	/** Regular JUnit console command line options. */
	private CommandLineOptions commandLineOptions;

	/** Constructor. */
	public ImpactedTestsExecutorCommandLineOptions(CommandLineOptions jUnitResult) {
		commandLineOptions = jUnitResult;
	}

	/** Whether help should be displayed.  */
	public boolean isDisplayHelp() {
		return commandLineOptions.isDisplayHelp();
	}

	/** At which detail level and form test summary should be printed to the console. */
	public Details getDetails() {
		return commandLineOptions.getDetails();
	}

	/** Which theme to use e.g. whether to use unicode or ascii art to print tree structures in the test summary. */
	public Theme getTheme() {
		return commandLineOptions.getTheme();
	}

	/** Whether to disable ansi colors in console output. */
	public boolean isAnsiColorOutputDisabled() {
		return commandLineOptions.isAnsiColorOutputDisabled();
	}

	/** The dir in which to write the JUnit and test details reports. */
	public Optional<Path> getReportsDir() {
		return commandLineOptions.getReportsDir();
	}

	/** The normal JUnit command line options object. */
	public CommandLineOptions toJUnitOptions() {
		return commandLineOptions;
	}
}
