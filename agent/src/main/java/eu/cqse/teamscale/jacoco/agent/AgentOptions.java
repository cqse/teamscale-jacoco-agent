/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import eu.cqse.teamscale.jacoco.agent.commandline.Validator;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.store.file.TimestampedFileStore;
import eu.cqse.teamscale.jacoco.agent.store.upload.http.HttpUploadStore;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.TeamscaleServer;
import eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.TeamscaleUploadStore;
import eu.cqse.teamscale.jacoco.agent.testimpact.TestImpactAgent;
import eu.cqse.teamscale.report.testwise.jacoco.cache.CoverageGenerationException;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.collections.PairList;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.runtime.WildcardMatcher;
import org.jacoco.report.JavaNames;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static eu.cqse.teamscale.jacoco.agent.store.upload.teamscale.ITeamscaleService.EReportFormat.TESTWISE_COVERAGE;

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
	 * Include patterns to apply during JaCoCo's traversal of class files. If null
	 * then everything is included.
	 */
	/* package */ WildcardMatcher locationIncludeFilters = null;

	/**
	 * Exclude patterns to apply during JaCoCo's traversal of class files. If null
	 * then nothing is excluded.
	 */
	/* package */ WildcardMatcher locationExcludeFilters = null;

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
	 * The report artifacts that should be produced and stored.
	 * Only applies for the Test Impact mode.
	 */
	/* package */ Set<EReportFormat> httpServerReportFormats = CollectionUtils.asUnmodifiableHashSet(TESTWISE_COVERAGE);

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

		validator.isTrue(!useTestImpactMode() || uploadUrl == null, "'upload-url' option is " +
				"incompatible with Test Impact mode!");

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
		StringBuilder builder = new StringBuilder("output=none");
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

	/**
	 * Returns in instance of the agent that was configured. Either an agent with interval based line-coverage dump or
	 * the HTTP server is used.
	 */
	public AgentBase createAgent() throws CoverageGenerationException {
		if (useTestImpactMode()) {
			return new TestImpactAgent(this);
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
	 * @see #dumpIntervalInMinutes
	 */
	public int getDumpIntervalInMillis() {
		return dumpIntervalInMinutes * 60_000;
	}

	/**
	 * @see #shouldIgnoreDuplicateClassFiles
	 */
	public boolean shouldIgnoreDuplicateClassFiles() {
		return shouldIgnoreDuplicateClassFiles;
	}

	/** Returns whether the config indicates to use Test Impact mode. */
	private boolean useTestImpactMode() {
		return httpServerPort != null;
	}

	/**
	 * Returns a set of report formats that the server should produce and dump to the store.
	 */
	public Set<EReportFormat> getHttpServerReportFormats() {
		return httpServerReportFormats;
	}

	/**
	 * Returns the port at which the http server should listen for test execution information or null if disabled.
	 */
	public Integer getHttpServerPort() {
		return httpServerPort;
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
	 * @see #locationIncludeFilters
	 * @see #locationExcludeFilters
	 */
	public Predicate<String> getLocationIncludeFilter() {
		return path -> {
			String className = getClassName(path);
			// first check includes
			if (locationIncludeFilters != null && !locationIncludeFilters.matches(className)) {
				return false;
			}
			// if they match, check excludes
			return locationExcludeFilters == null || !locationExcludeFilters.matches(className);
		};
	}

	/**
	 * Returns the normalized class name of the given class file's path.
	 */
	/* package */
	static String getClassName(String path) {
		String[] parts = FileSystemUtils.normalizeSeparators(path).split("@");
		if (parts.length == 0) {
			return "";
		}

		String pathInsideJar = parts[parts.length - 1];
		String pathWithoutExtension = StringUtils.removeLastPart(pathInsideJar, '.');
		return new JavaNames().getQualifiedClassName(pathWithoutExtension);
	}
}
