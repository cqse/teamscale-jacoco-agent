/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent;

import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.commandline.Validator;
import com.teamscale.jacoco.agent.git_properties.GitPropertiesLocatingTransformer;
import com.teamscale.jacoco.agent.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.store.IXmlStore;
import com.teamscale.jacoco.agent.store.UploadStoreException;
import com.teamscale.jacoco.agent.store.TimestampedFileStore;
import com.teamscale.jacoco.agent.store.upload.azure.AzureFileStorageConfig;
import com.teamscale.jacoco.agent.store.upload.azure.AzureFileStorageUploadStore;
import com.teamscale.jacoco.agent.store.upload.delay.DelayedCommitDescriptorStore;
import com.teamscale.jacoco.agent.store.upload.http.HttpUploadStore;
import com.teamscale.jacoco.agent.store.upload.teamscale.TeamscaleUploadStore;
import com.teamscale.jacoco.agent.testimpact.TestExecutionWriter;
import com.teamscale.jacoco.agent.testimpact.TestwiseCoverageAgent;
import com.teamscale.jacoco.agent.util.AgentUtils;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.PairList;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses agent command line options.
 */
public class AgentOptions {

	private final Logger logger = LoggingUtils.getLogger(this);

	/**
	 * The original options passed to the agent.
	 */
	/* package */ String originalOptionsString;

	/**
	 * The directories and/or zips that contain all class files being profiled. Never null. If this is empty, classes
	 * should be dumped to a temporary directory which should be used as the class-dir.
	 */
	/* package */ List<File> classDirectoriesOrZips = new ArrayList<>();

	/**
	 * The logging configuration file.
	 */
	/* package */ Path loggingConfig = null;

	/**
	 * The directory to write the XML traces to.
	 */
	/* package */ Path outputDirectory = AgentUtils.getAgentDirectory().resolve("coverage");

	/**
	 * The URL to which to upload coverage zips.
	 */
	/* package */ HttpUrl uploadUrl = null;

	/**
	 * Additional meta data files to upload together with the coverage XML.
	 */
	/* package */ List<Path> additionalMetaDataFiles = new ArrayList<>();

	/** Whether the agent should be run in testwise coverage mode or normal mode. */
	/* package */ EMode mode = EMode.NORMAL;

	/**
	 * The interval in minutes for dumping XML data.
	 */
	/* package */ int dumpIntervalInMinutes = 60;


	/** Whether to dump coverage when the JVM shuts down. */
	/* package */ boolean shouldDumpOnExit = true;

	/**
	 * Whether to validate SSL certificates or simply ignore them. We disable this by default on purpose in order to
	 * make the initial setup of the agent as smooth as possible. Many users have self-signed certificates that cause
	 * problems. Users that need this feature can turn it on deliberately.
	 */
	/* package */ boolean validateSsl = false;

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
	 * The configuration necessary to upload files to an azure file storage
	 */
	/* package */ AzureFileStorageConfig azureFileStorageConfig = new AzureFileStorageConfig();

	/**
	 * @see #originalOptionsString
	 */
	public String getOriginalOptionsString() {
		return originalOptionsString;
	}

	/**
	 * Validates the options and returns a validator with all validation errors.
	 */
	/* package */ Validator getValidator() {
		Validator validator = new Validator();
		for (File path : classDirectoriesOrZips) {
			validator.isTrue(path.exists(), "Path '" + path + "' does not exist");
			validator.isTrue(path.canRead(), "Path '" + path + "' is not readable");
		}

		validator.ensure(() -> {
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
				CCSMAssert.isTrue("xml".equalsIgnoreCase(FileSystemUtils.getFileExtension(loggingConfig.toFile())),
						"The logging configuration file must have the file extension .xml and be a valid XML file");
			});
		}

		validator.isTrue(!useTestwiseCoverageMode() || uploadUrl == null, "'upload-url' option is " +
				"incompatible with Testwise coverage mode!");

		validator.isFalse(uploadUrl == null && !additionalMetaDataFiles.isEmpty(),
				"You specified additional meta data files to be uploaded but did not configure an upload URL");

		validator.isTrue(teamscaleServer.hasAllRequiredFieldsNull() || teamscaleServer.hasAllRequiredFieldsSet(),
				"You did provide some options prefixed with 'teamscale-', but not all required ones!");

		validator.isTrue((azureFileStorageConfig.hasAllRequiredFieldsSet() || azureFileStorageConfig
						.hasAllRequiredFieldsNull()),
				"If you want to upload data to an Azure file storage you need to provide both " +
						"'azure-url' and 'azure-key' ");

		List<Boolean> configuredStores = Stream
				.of(azureFileStorageConfig.hasAllRequiredFieldsSet(), teamscaleServer.hasAllRequiredFieldsSet(),
						uploadUrl != null).filter(x -> x).collect(Collectors.toList());

		validator.isTrue(configuredStores.size() <= 1, "You cannot configure multiple upload stores, " +
				"such as a Teamscale instance, upload URL or Azure file storage");

		return validator;
	}

	/**
	 * Returns the options to pass to the JaCoCo agent.
	 */
	public String createJacocoAgentOptions() throws AgentOptionParseException {
		StringBuilder builder = new StringBuilder(getModeSpecificOptions());
		if (jacocoIncludes != null) {
			builder.append(",includes=").append(jacocoIncludes);
		}
		if (jacocoExcludes != null) {
			builder.append(",excludes=").append(jacocoExcludes);
		}

		if (classDirectoriesOrZips.isEmpty()) {
			Path tempDir = createTemporaryDumpDirectory();
			tempDir.toFile().deleteOnExit();
			builder.append(",classdumpdir=").append(tempDir.toAbsolutePath().toString());

			classDirectoriesOrZips = Collections.singletonList(tempDir.toFile());
		}

		additionalJacocoOptions.forEach((key, value) -> builder.append(",").append(key).append("=").append(value));

		return builder.toString();
	}

	private Path createTemporaryDumpDirectory() throws AgentOptionParseException {
		try {
			return Files.createTempDirectory("jacoco-class-dump");
		} catch (IOException e) {
			logger.warn("Unable to create temporary directory in default location. Trying in output directory.");
		}

		try {
			return Files.createTempDirectory(outputDirectory, "jacoco-class-dump");
		} catch (IOException e) {
			logger.warn("Unable to create temporary directory in output directory. Trying in agent's directory.");
		}

		Path agentDirectory = AgentUtils.getAgentDirectory();
		if (agentDirectory == null) {
			throw new AgentOptionParseException("Could not resolve directory that contains the agent");
		}
		try {
			return Files.createTempDirectory(agentDirectory, "jacoco-class-dump");
		} catch (IOException e) {
			throw new AgentOptionParseException("Unable to create a temporary directory anywhere", e);
		}
	}

	/** Sets output to none for normal mode and destination file in testwise coverage mode */
	private String getModeSpecificOptions() {
		if (useTestwiseCoverageMode()) {
			return "sessionid=,destfile=" + getTempFile("jacoco", "exec").getAbsolutePath();
		} else {
			return "output=none";
		}
	}

	private File getTempFile(final String prefix, final String extension) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.SSS");
		return new File(outputDirectory.toFile(),
				prefix + "-" + dateFormat.format(new Date()) + "." + extension);
	}

	/**
	 * Returns in instance of the agent that was configured. Either an agent with interval based line-coverage dump or
	 * the HTTP server is used.
	 */
	public AgentBase createAgent(Instrumentation instrumentation) throws UploadStoreException {
		if (useTestwiseCoverageMode()) {
			return new TestwiseCoverageAgent(this, new TestExecutionWriter(getTempFile("test-execution", "json")));
		} else {
			return new Agent(this, instrumentation);
		}
	}

	/**
	 * Creates the store to use for the coverage XMLs.
	 */
	public IXmlStore createStore(Instrumentation instrumentation) throws UploadStoreException {
		TimestampedFileStore fileStore = new TimestampedFileStore(outputDirectory);
		if (uploadUrl != null) {
			return new HttpUploadStore(fileStore, uploadUrl, additionalMetaDataFiles);
		}
		if (teamscaleServer.hasAllRequiredFieldsSet()) {
			if (teamscaleServer.commit == null) {
				logger.info("You did not provide a commit to upload to directly, so the Agent will try and" +
						" auto-detect it by searching all profiled Jar/War/Ear/... files for a git.properties file.");
				TimestampedFileStore cacheStore = new TimestampedFileStore(outputDirectory);
				DelayedCommitDescriptorStore store = new DelayedCommitDescriptorStore(
						commit -> new TeamscaleUploadStore(fileStore, teamscaleServer), cacheStore);
				GitPropertiesLocator locator = new GitPropertiesLocator(store);
				instrumentation.addTransformer(new GitPropertiesLocatingTransformer(locator));
				return store;
			}
			return new TeamscaleUploadStore(fileStore, teamscaleServer);
		}

		if (azureFileStorageConfig.hasAllRequiredFieldsSet()) {
			return new AzureFileStorageUploadStore(fileStore, azureFileStorageConfig,
					additionalMetaDataFiles);
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
	public EDuplicateClassFileBehavior getDuplicateClassFileBehavior() {
		if (shouldIgnoreDuplicateClassFiles) {
			return EDuplicateClassFileBehavior.WARN;
		} else {
			return EDuplicateClassFileBehavior.FAIL;
		}
	}

	/** Returns whether the config indicates to use Test Impact mode. */
	private boolean useTestwiseCoverageMode() {
		return mode == EMode.TESTWISE;
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
	 * @see #validateSsl
	 */
	public boolean shouldValidateSsl() {
		return validateSsl;
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

	/** Whether coverage should be dumped on JVM shutdown. */
	public boolean shouldDumpOnExit() {
		return shouldDumpOnExit;
	}

	/** Describes the two possible modes the agent can be started in. */
	public enum EMode {

		/**
		 * The default mode which produces JaCoCo XML coverage files on exit, in a defined interval or when triggered
		 * via an HTTP endpoint. Each dump produces a new file containing the all collected coverage.
		 */
		NORMAL,

		/**
		 * Testwise coverage mode in which the agent only dumps when triggered via an HTTP endpoint. Coverage is written
		 * as exec and appended into a single file.
		 */
		TESTWISE
	}
}
