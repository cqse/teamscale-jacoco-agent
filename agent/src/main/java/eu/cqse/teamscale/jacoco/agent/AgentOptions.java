/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.client.TeamscaleServer;
import eu.cqse.teamscale.jacoco.agent.commandline.Validator;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.store.file.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.agent.store.upload.http.HttpUploadStore;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.TeamscaleUploadStore;
import eu.cqse.teamscale.jacoco.agent.testimpact.TestwiseCoverageAgent;
import eu.cqse.teamscale.report.util.ClasspathWildcardIncludeFilter;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.PairList;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Parses agent command line options.
 */
public class AgentOptions {

	/**
	 * The original options passed to the agent.
	 */
	/* package */ String originalOptionsString;

	/**
	 * The directories and/or zips that contain all class files being profiled.
	 */
	/* package */ List<File> classDirectoriesOrZips = new ArrayList<>();

	/**
	 * The logging configuration file.
	 */
	/* package */ Path loggingConfig = null;

	/**
	 * The directory to write the XML traces to.
	 */
	/* package */ Path outputDirectory = null;

	/**
	 * The URL to which to upload coverage zips.
	 */
	/* package */ HttpUrl uploadUrl = null;

	/**
	 * Additional meta data files to upload together with the coverage XML.
	 */
	/* package */ List<Path> additionalMetaDataFiles = new ArrayList<>();

	/**
	 * The interval in minutes for dumping XML data.
	 */
	/* package */ int dumpIntervalInMinutes = 60;

	/**
	 * Whether to ignore duplicate, non-identical class files.
	 */
	/* package */ boolean shouldIgnoreDuplicateClassFiles = true;

	/**
	 * Include patterns to pass on to JaCoCo.
	 */
	/* package */ String jacocoIncludes = null;

	/**
	 * Exclude patterns to pass on to JaCoCo.
	 */
	/* package */ String jacocoExcludes = null;

	/**
	 * Additional user-provided options to pass to JaCoCo.
	 */
	/* package */ PairList<String, String> additionalJacocoOptions = new PairList<>();

	/**
	 * The teamscale server to which coverage should be uploaded.
	 */
	/* package */ TeamscaleServer teamscaleServer = new TeamscaleServer();

	/**
	 * The name of the environment variable that holds the test uniform path.
	 */
	/* package */ String testEnvironmentVariable = null;

	/**
	 * The port on which the HTTP server should be listening.
	 */
	/* package */ Integer httpServerPort = null;

	/**
	 * @see #originalOptionsString
	 */
	public String getOriginalOptionsString() {
		return originalOptionsString;
	}

	/**
	 * Validates the options and throws an exception if they're not valid.
	 */
	/* package */ Validator getValidator() {
		Validator validator = new Validator();

		validator.isFalse(getClassDirectoriesOrZips().isEmpty(),
				"You must specify at least one directory or zip that contains class files");
		for (File path : classDirectoriesOrZips) {
			validator.isTrue(path.exists(), "Path '" + path + "' does not exist");
			validator.isTrue(path.canRead(), "Path '" + path + "' is not readable");
		}

		validator.ensure(() -> {
			CCSMAssert.isNotNull(outputDirectory, "You must specify an output directory");
			FileSystemUtils.ensureDirectoryExists(outputDirectory.toFile());
		});

		if (loggingConfig != null) {
			validator.ensure(() -> {
				CCSMAssert.isTrue(Files.exists(loggingConfig),
						"The path provided for the logging configuration does not exist: " + loggingConfig);
				CCSMAssert.isTrue(Files.isRegularFile(loggingConfig),
						"The path provided for the logging configuration is not a file: " + loggingConfig);
				CCSMAssert.isTrue(Files.isReadable(loggingConfig),
						"The file provided for the logging configuration is not readable: " + loggingConfig);
				CCSMAssert.isTrue(FileSystemUtils.getFileExtension(loggingConfig.toFile()).equalsIgnoreCase("xml"),
						"The logging configuration file must have the file extension .xml and be a valid XML file");
			});
		}

		validator.isTrue(!useTestwiseCoverageMode() || uploadUrl == null, "'upload-url' option is " +
				"incompatible with Testwise coverage mode!");

		validator.isFalse(uploadUrl == null && !additionalMetaDataFiles.isEmpty(),
				"You specified additional meta data files to be uploaded but did not configure an upload URL");

		validator.isTrue(teamscaleServer.hasAllRequiredFieldsNull() || teamscaleServer.hasAllRequiredFieldsSet(),
				"You did provide some options prefixed with 'teamscale-', but not all required ones!");

		validator.isTrue(uploadUrl == null || teamscaleServer.hasAllRequiredFieldsNull(),
				"You did provide 'upload-url' and some 'teamscale-' option at the same time, but only one of " +
						"them can be used!");
		return validator;
	}

	/**
	 * Returns the options to pass to the JaCoCo agent.
	 */
	public String createJacocoAgentOptions() {
		StringBuilder builder = new StringBuilder(getModeSpecificOptions());
		if (jacocoIncludes != null) {
			builder.append(",includes=").append(jacocoIncludes);
		}
		if (jacocoExcludes != null) {
			builder.append(",excludes=").append(jacocoExcludes);
		}

		additionalJacocoOptions.forEach((key, value) -> {
			builder.append(",").append(key).append("=").append(value);
		});

		return builder.toString();
	}

	private String getModeSpecificOptions() {
		if (!useTestwiseCoverageMode()) {
			return "output=none";
		} else {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("destfile=" + new File(outputDirectory.toFile(), "jacoco.exec").getAbsolutePath());
			if (testEnvironmentVariable == null) {
				stringBuilder.append(",append=false");
			}
			return stringBuilder.toString();
		}
	}

	/**
	 * Returns in instance of the agent that was configured. Either an agent with interval based line-coverage dump or
	 * the HTTP server is used.
	 */
	public AgentBase createAgent() {
		if (useTestwiseCoverageMode()) {
			return new TestwiseCoverageAgent(this);
		} else {
			return new Agent(this);
		}
	}

	/**
	 * Creates the store to use for the coverage XMLs.
	 */
	public IXmlStore createStore() {
		TimestampedFileStore fileStore = new TimestampedFileStore(outputDirectory);
		if (uploadUrl != null) {
			return new HttpUploadStore(fileStore, uploadUrl, additionalMetaDataFiles);
		}
		if (teamscaleServer.hasAllRequiredFieldsSet()) {
			return new TeamscaleUploadStore(fileStore, teamscaleServer);
		}
		return fileStore;
	}

	/**
	 * @see #classDirectoriesOrZips
	 */
	public List<File> getClassDirectoriesOrZips() {
		return classDirectoriesOrZips;
	}

	/** @see #teamscaleServer */
	public TeamscaleServer getTeamscaleServerOptions() {
		return teamscaleServer;
	}

	/**
	 * @see #dumpIntervalInMinutes
	 */
	public int getDumpIntervalInMinutes() {
		return dumpIntervalInMinutes;
	}

	/**
	 * @see #shouldIgnoreDuplicateClassFiles
	 */
	public boolean shouldIgnoreDuplicateClassFiles() {
		return shouldIgnoreDuplicateClassFiles;
	}

	/** Returns whether the config indicates to use Test Impact mode. */
	private boolean useTestwiseCoverageMode() {
		return httpServerPort != null || testEnvironmentVariable != null;
	}

	/**
	 * Returns the port at which the http server should listen for test execution information or null if disabled.
	 */
	public Integer getHttpServerPort() {
		return httpServerPort;
	}

	/**
	 * Returns the name of the environment variable to read the test uniform path from.
	 */
	public String getTestEnvironmentVariableName() {
		return testEnvironmentVariable;
	}

	/**
	 * @see #loggingConfig
	 */
	public Path getLoggingConfig() {
		return loggingConfig;
	}

	/**
	 * @see #loggingConfig
	 */
	public void setLoggingConfig(Path loggingConfig) {
		this.loggingConfig = loggingConfig;
	}

	/**
	 * @see #jacocoIncludes
	 * @see #jacocoExcludes
	 */
	public Predicate<String> getLocationIncludeFilter() {
		return new ClasspathWildcardIncludeFilter(jacocoIncludes, jacocoExcludes);
	}

	/** Whether coverage should be dumped in regular intervals. */
	public boolean shouldDumpInIntervals() {
		return dumpIntervalInMinutes > 0;
	}
}
