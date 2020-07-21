/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.options;

import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.Agent;
import com.teamscale.jacoco.agent.AgentBase;
import com.teamscale.jacoco.agent.commandline.Validator;
import com.teamscale.jacoco.agent.git_properties.GitPropertiesLocatingTransformer;
import com.teamscale.jacoco.agent.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.testimpact.TestExecutionWriter;
import com.teamscale.jacoco.agent.testimpact.TestwiseCoverageAgent;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.upload.LocalDiskUploader;
import com.teamscale.jacoco.agent.upload.UploaderException;
import com.teamscale.jacoco.agent.upload.azure.AzureFileStorageConfig;
import com.teamscale.jacoco.agent.upload.azure.AzureFileStorageUploader;
import com.teamscale.jacoco.agent.upload.delay.DelayedCommitDescriptorUploader;
import com.teamscale.jacoco.agent.upload.http.HttpUploader;
import com.teamscale.jacoco.agent.upload.teamscale.TeamscaleUploader;
import com.teamscale.jacoco.agent.util.AgentUtils;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.testwise.jacoco.JaCoCoTestwiseReportGenerator;
import com.teamscale.report.util.ClasspathWildcardIncludeFilter;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.PairList;
import org.jacoco.core.runtime.WildcardMatcher;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses agent command line options.
 */
public class AgentOptions {
	/**
	 * Can be used to format {@link LocalDate} to the format "yyyy-MM-dd-HH-mm-ss.SSS"
	 */
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd-HH-mm-ss.SSS", Locale.ENGLISH);

	/** Option name that allows to specify to which branch coverage should be uploaded to (branch:timestamp). */
	public static final String TEAMSCALE_COMMIT_OPTION = "teamscale-commit";

	/** Option name that allows to specify a git commit hash to which coverage should be uploaded to. */
	public static final String TEAMSCALE_REVISION_OPTION = "teamscale-revision";

	/** Option name that allows to specify a jar file that contains the branch name and timestamp in a MANIFEST.MF file. */
	public static final String TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION = "teamscale-commit-manifest-jar";

	/** Option name that allows to specify a jar file that contains the git commit hash in a git.properties file. */
	public static final String TEAMSCALE_GIT_PROPERTIES_JAR_OPTION = "teamscale-git-properties-jar";

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
	private Path outputDirectory;

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
	/* package */ int dumpIntervalInMinutes = 480;

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
	/* package */ EDuplicateClassFileBehavior duplicateClassFileBehavior = EDuplicateClassFileBehavior.WARN;

	/**
	 * Include patterns for fully qualified class names to pass on to JaCoCo. See {@link WildcardMatcher} for the
	 * pattern syntax. Individual patterns must be separated by ":".
	 */
	/* package */ String jacocoIncludes = null;

	/**
	 * Exclude patterns for fully qualified class names to pass on to JaCoCo. See {@link WildcardMatcher} for the
	 * pattern syntax. Individual patterns must be separated by ":".
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
	 * The name of the environment variable that holds the test uniform path for TIA mode.
	 */
	/* package */ String testEnvironmentVariable = null;

	/**
	 * The port on which the HTTP server should be listening.
	 */
	/* package */ Integer httpServerPort = null;

	/**
	 * How testwise coverage should be handled in test-wise mode.
	 */
	/* package */ ETestwiseCoverageMode testwiseCoverageMode = ETestwiseCoverageMode.EXEC_FILE;


	/**
	 * Whether classes without coverage should be skipped from the XML report.
	 */
	/* package */ boolean ignoreUncoveredClasses = false;

	/**
	 * The configuration necessary to upload files to an azure file storage
	 */
	/* package */ AzureFileStorageConfig azureFileStorageConfig = new AzureFileStorageConfig();

	public AgentOptions() {
		setParentOutputDirectory(AgentUtils.getAgentDirectory().resolve("coverage"));
	}

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

		validator.isFalse(uploadUrl == null && !additionalMetaDataFiles.isEmpty(),
				"You specified additional meta data files to be uploaded but did not configure an upload URL");

		validator.isTrue(teamscaleServer.hasAllRequiredFieldsNull() || teamscaleServer.hasAllRequiredFieldsSet(),
				"You did provide some options prefixed with 'teamscale-', but not all required ones!");

		validator.isTrue(teamscaleServer.revision == null || teamscaleServer.commit == null,
				"'teamscale-revision' is incompatible with '" + AgentOptions.TEAMSCALE_COMMIT_OPTION + "' and '" +
						AgentOptions.TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION + "'.");

		validator.isTrue((azureFileStorageConfig.hasAllRequiredFieldsSet() || azureFileStorageConfig
						.hasAllRequiredFieldsNull()),
				"If you want to upload data to an Azure file storage you need to provide both " +
						"'azure-url' and 'azure-key' ");

		List<Boolean> configuredStores = Stream
				.of(azureFileStorageConfig.hasAllRequiredFieldsSet(), teamscaleServer.hasAllRequiredFieldsSet(),
						uploadUrl != null).filter(x -> x).collect(Collectors.toList());

		validator.isTrue(configuredStores.size() <= 1, "You cannot configure multiple upload stores, " +
				"such as a Teamscale instance, upload URL or Azure file storage");

		appendTestwiseCoverageValidations(validator);

		return validator;
	}

	private void appendTestwiseCoverageValidations(Validator validator) {
		validator.isFalse(
				useTestwiseCoverageMode() && httpServerPort == null && testEnvironmentVariable == null,
				"You use 'mode' 'TESTWISE' but did use neither 'http-server-port' nor 'test-env'!" +
						" One of them is required!");

		validator.isFalse(useTestwiseCoverageMode() && httpServerPort != null && testEnvironmentVariable != null,
				"You did set both 'http-server-port' and 'test-env'! Only one of them is allowed!");

		validator.isFalse(useTestwiseCoverageMode() && uploadUrl != null, "'upload-url' option is " +
				"incompatible with Testwise coverage mode!");

		validator.isFalse(testwiseCoverageMode == ETestwiseCoverageMode.TEAMSCALE_UPLOAD
						&& !teamscaleServer.hasAllRequiredFieldsSet(),
				"You use 'tia-mode=teamscale-upload' but did not set all required 'teamscale-' fields to facilitate" +
						" a connection to Teamscale!");

		validator.isFalse(!useTestwiseCoverageMode() && testEnvironmentVariable != null,
				"You use 'test-env' but did not set 'mode' to 'TESTWISE'!");
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

		// Don't dump class files in testwise mode when coverage is written to an exec file
		boolean needsClassFiles = mode == EMode.NORMAL || testwiseCoverageMode != ETestwiseCoverageMode.EXEC_FILE;
		if (classDirectoriesOrZips.isEmpty() && needsClassFiles) {
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

	/**
	 * Returns additional options for JaCoCo depending on the selected {@link #mode} and {@link #testwiseCoverageMode}.
	 */
	private String getModeSpecificOptions() {
		if (useTestwiseCoverageMode() && testwiseCoverageMode == ETestwiseCoverageMode.EXEC_FILE) {
			// when writing to a .exec file, we can instruct JaCoCo to do so directly
			return "sessionid=,destfile=" + getTempFile("jacoco", "exec").getAbsolutePath();
		} else {
			// otherwise we don't need JaCoCo to perform any output of the .exec information
			return "output=none";
		}
	}

	private File getTempFile(final String prefix, final String extension) {
		return new File(outputDirectory.toFile(),
				prefix + "-" + LocalDateTime.now().format(DATE_TIME_FORMATTER) + "." + extension);
	}

	/**
	 * Returns in instance of the agent that was configured. Either an agent with interval based line-coverage dump or
	 * the HTTP server is used.
	 */
	public AgentBase createAgent(Instrumentation instrumentation) throws UploaderException {
		if (useTestwiseCoverageMode()) {
			JaCoCoTestwiseReportGenerator reportGenerator = new JaCoCoTestwiseReportGenerator(
					getClassDirectoriesOrZips(), getLocationIncludeFilter(),
					getDuplicateClassFileBehavior(), LoggingUtils.wrap(logger));
			return new TestwiseCoverageAgent(this, new TestExecutionWriter(getTempFile("test-execution", "json")),
					reportGenerator);
		} else {
			return new Agent(this, instrumentation);
		}
	}


	/**
	 * Creates a {@link TeamscaleClient} based on the agent options. Returns null if the user did not fully configure a
	 * Teamscale connection.
	 */
	public TeamscaleClient createTeamscaleClient() {
		if (teamscaleServer.hasAllRequiredFieldsSet()) {
			return new TeamscaleClient(teamscaleServer.url.toString(), teamscaleServer.userName,
					teamscaleServer.userAccessToken, teamscaleServer.project);
		}
		return null;
	}

	/**
	 * Creates an uploader for the coverage XMLs.
	 */
	public IUploader createUploader(Instrumentation instrumentation) throws UploaderException {
		if (uploadUrl != null) {
			return new HttpUploader(uploadUrl, additionalMetaDataFiles);
		}
		if (teamscaleServer.hasAllRequiredFieldsSet()) {
			if (!teamscaleServer.hasCommitOrRevision()) {
				logger.info("You did not provide a commit to upload to directly, so the Agent will try and" +
						" auto-detect it by searching all profiled Jar/War/Ear/... files for a git.properties file.");
				return createDelayedTeamscaleUploader(instrumentation);
			}
			return new TeamscaleUploader(teamscaleServer);
		}

		if (azureFileStorageConfig.hasAllRequiredFieldsSet()) {
			return new AzureFileStorageUploader(azureFileStorageConfig,
					additionalMetaDataFiles);
		}

		return new LocalDiskUploader();
	}

	private IUploader createDelayedTeamscaleUploader(Instrumentation instrumentation) {
		DelayedCommitDescriptorUploader store = new DelayedCommitDescriptorUploader(
				revision -> {
					teamscaleServer.revision = revision;
					return new TeamscaleUploader(teamscaleServer);
				}, outputDirectory);
		GitPropertiesLocator locator = new GitPropertiesLocator(store);
		instrumentation.addTransformer(new GitPropertiesLocatingTransformer(locator, getLocationIncludeFilter()));
		return store;
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
	 * Get the directory to which the coverage files are written to
	 */
	public Path getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * Sets the parent of the output directory for this run. The output directory itself will be created in this folder
	 * is named after the current timestamp with the format yyyy-MM-dd-HH-mm-ss.SSS
	 */
	public void setParentOutputDirectory(Path outputDirectoryParent) {
		outputDirectory = outputDirectoryParent.resolve(LocalDateTime.now().format(DATE_TIME_FORMATTER));
	}

	/**
	 * @see #dumpIntervalInMinutes
	 */
	public int getDumpIntervalInMinutes() {
		return dumpIntervalInMinutes;
	}

	/**
	 * @see #duplicateClassFileBehavior
	 */
	public EDuplicateClassFileBehavior getDuplicateClassFileBehavior() {
		return duplicateClassFileBehavior;
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
	public ClasspathWildcardIncludeFilter getLocationIncludeFilter() {
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

	/** @see AgentOptions#testwiseCoverageMode */
	public ETestwiseCoverageMode getTestwiseCoverageMode() {
		return testwiseCoverageMode;
	}

	/** @see #ignoreUncoveredClasses */
	public boolean shouldIgnoreUncoveredClasses() {
		return ignoreUncoveredClasses;
	}
}
