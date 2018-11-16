package org.junit.platform.console.options;

import eu.cqse.teamscale.client.CommitDescriptor;
import okhttp3.HttpUrl;
import org.junit.platform.console.shadow.joptsimple.OptionParser;
import org.junit.platform.console.shadow.joptsimple.OptionSet;
import org.junit.platform.console.shadow.joptsimple.OptionSpec;

import static java.util.Arrays.asList;

/** Helper class to parse command line options. */
public class AvailableImpactedTestsExecutorCommandLineOptions {

	/** Available options from jUnit */
	private AvailableOptions jUnitOptions;

	/** Holds the command line parser with the standard jUnit options and ours. */
	private final OptionParser parser;

	/** Teamscale server options */
	private final OptionSpec<String> url;
	private final OptionSpec<String> project;
	private final OptionSpec<String> userName;
	private final OptionSpec<String> userAccessToken;
	private final OptionSpec<String> partition;

	private final OptionSpec<String> baseline;
	private final OptionSpec<String> end;

	private final OptionSpec<Void> runAllTests;

	private OptionSpec<String> agentUrl;

	/** Constructor. */
	AvailableImpactedTestsExecutorCommandLineOptions() {
		jUnitOptions = new AvailableOptions();
		parser = jUnitOptions.getParser();
		url = parser.accepts("url",
				"Url of the teamscale server")
				.withRequiredArg();

		project = parser.accepts("project",
				"Project ID of the teamscale project")
				.withRequiredArg();

		userName = parser.accepts("user",
				"The user name in teamscale")
				.withRequiredArg();

		userAccessToken = parser.accepts("access-token",
				"The users access token for Teamscale")
				.withRequiredArg();

		partition = parser.accepts("partition",
				"Partition of the tests")
				.withRequiredArg();

		baseline = parser.accepts("baseline",
				"The baseline commit")
				.withRequiredArg();

		end = parser.accepts("end",
				"The end commit")
				.withRequiredArg();

		runAllTests = parser.acceptsAll(asList("all", "run-all-tests"),
				"Partition of the tests");

		agentUrl = parser.accepts("agent-url",
				"Url of the teamscale jacoco agent that generates coverage for the system")
				.withRequiredArg();
	}

	/** Returns an options parser with the available options set. */
	public OptionParser getParser() {
		return parser;
	}

	/** Converts the parsed parameters into a {@link ImpactedTestsExecutorCommandLineOptions} object. */
	public ImpactedTestsExecutorCommandLineOptions toCommandLineOptions(OptionSet detectedOptions) {
		CommandLineOptions jUnitResult = jUnitOptions.toCommandLineOptions(detectedOptions);
		ImpactedTestsExecutorCommandLineOptions result = new ImpactedTestsExecutorCommandLineOptions(jUnitResult);

		result.server.url = detectedOptions.valueOf(this.url);
		result.server.project = detectedOptions.valueOf(this.project);
		result.server.userName = detectedOptions.valueOf(this.userName);
		result.server.userAccessToken = detectedOptions.valueOf(this.userAccessToken);
		result.partition = detectedOptions.valueOf(this.partition);

		result.runAllTests = detectedOptions.has(this.runAllTests);

		if (detectedOptions.has(this.baseline)) {
			result.baseline = Long.parseLong(detectedOptions.valueOf(this.baseline));
		}
		result.endCommit = CommitDescriptor.parse(detectedOptions.valueOf(this.end));

		result.agentUrl = HttpUrl.parse(detectedOptions.valueOf(this.agentUrl));

		return result;
	}
}

