package org.junit.platform.console.options;

import com.teamscale.client.CommitDescriptor;
import okhttp3.HttpUrl;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Holds the actual values of the options configured via command line parameters. */
public class ImpactedTestsExecutorCommandLineOptions {

	/** Connection details of the Teamscale server. */
	private final Server server = new Server();

	/** The partition to upload test details to and get impacted tests from. */
	private String partition;

	/** Executes all tests, not only impacted ones if set. */
	private boolean runAllTests;

	/** The baseline commit used for TIA. */
	private Long baseline;

	/** The end commit used for TIA and for uploading the coverage. */
	private CommitDescriptor endCommit;

	/** Regular JUnit console command line options. */
	private final CommandLineOptions commandLineOptions;

	/** The url (including port) at which the agent listens. */
	private List<HttpUrl> agentUrls;

	/** Constructor. */
	public ImpactedTestsExecutorCommandLineOptions(CommandLineOptions jUnitResult) {
		commandLineOptions = jUnitResult;
	}

	/** Whether help should be displayed. */
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

	/** @see #server */
	public Server getServer() {
		return server;
	}

	/** @see #partition */
	public String getPartition() {
		return partition;
	}

	/** @see #partition */
	public void setPartition(String partition) {
		this.partition = partition;
	}

	/** @see #runAllTests */
	public boolean isRunAllTests() {
		return runAllTests;
	}

	/** @see #runAllTests */
	public void setRunAllTests(boolean runAllTests) {
		this.runAllTests = runAllTests;
	}

	/** @see #baseline */
	public Long getBaseline() {
		return baseline;
	}

	/** @see #baseline */
	public void setBaseline(Long baseline) {
		this.baseline = baseline;
	}

	/** @see #endCommit */
	public CommitDescriptor getEndCommit() {
		return endCommit;
	}

	/** @see #endCommit */
	public void setEndCommit(CommitDescriptor endCommit) {
		this.endCommit = endCommit;
	}

	/** @see #commandLineOptions */
	public CommandLineOptions getCommandLineOptions() {
		return commandLineOptions;
	}

	/** @see #agentUrls */
	public List<HttpUrl> getAgentUrls() {
		return agentUrls;
	}

	/** @see #agentUrls */
	public void setAgentUrls(List<HttpUrl> agentUrl) {
		this.agentUrls = agentUrl;
	}

	/**
	 * Returns whether the service call should fail if the given commit is not yet processed by Teamscale.
	 * This is always enabled except for when the timestamp of the {@link #endCommit} is set to HEAD.
	 */
	public boolean shouldEnsureProcessed() {
		return !"HEAD".equals(this.endCommit.timestamp);
	}
}
