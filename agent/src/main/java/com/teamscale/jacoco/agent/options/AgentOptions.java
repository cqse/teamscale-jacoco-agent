/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.options;

import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.client.TeamscaleClient;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.jacoco.agent.commandline.Validator;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitMultiProjectPropertiesLocator;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocatingTransformer;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.commit_resolution.git_properties.GitPropertiesLocatorUtils;
import com.teamscale.jacoco.agent.commit_resolution.sapnwdi.NwdiMarkerClassLocatingTransformer;
import com.teamscale.jacoco.agent.options.sapnwdi.DelayedSapNwdiMultiUploader;
import com.teamscale.jacoco.agent.options.sapnwdi.SapNwdiApplications;
import com.teamscale.jacoco.agent.testimpact.TestImpactConfig;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.upload.LocalDiskUploader;
import com.teamscale.jacoco.agent.upload.UploaderException;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryConfig;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryUploader;
import com.teamscale.jacoco.agent.upload.azure.AzureFileStorageConfig;
import com.teamscale.jacoco.agent.upload.azure.AzureFileStorageUploader;
import com.teamscale.jacoco.agent.upload.delay.DelayedUploader;
import com.teamscale.jacoco.agent.upload.http.HttpUploader;
import com.teamscale.jacoco.agent.upload.teamscale.DelayedTeamscaleMultiProjectUploader;
import com.teamscale.jacoco.agent.upload.teamscale.TeamscaleConfig;
import com.teamscale.jacoco.agent.upload.teamscale.TeamscaleUploader;
import com.teamscale.jacoco.agent.util.AgentUtils;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.EDuplicateClassFileBehavior;
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
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Parses agent command line options.
 */
public class AgentOptions {

	/**
	 * Can be used to format {@link LocalDate} to the format "yyyy-MM-dd-HH-mm-ss.SSS"
	 */
	/* package */ static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd-HH-mm-ss.SSS", Locale.ENGLISH);

	/**
	 * The default excludes applied to JaCoCo. These are packages that should never be profiled. Excluding them makes
	 * debugging traces easier and reduces trace size and warnings about unmatched classes in Teamscale.
	 */
	public static final String DEFAULT_EXCLUDES = "shadow.*:com.sun.*:sun.*:org.eclipse.*:org.junit.*:junit.*:org.apache.*:org.slf4j.*:javax.*:org.gradle.*";

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
	 * Whether to validate SSL certificates, defaults to true.
	 */
	/* package */ boolean validateSsl = true;

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
	/* package */ String jacocoExcludes = DEFAULT_EXCLUDES;

	/**
	 * Additional user-provided options to pass to JaCoCo.
	 */
	/* package */ PairList<String, String> additionalJacocoOptions = new PairList<>();

	/**
	 * The teamscale server to which coverage should be uploaded.
	 */
	/* package */ TeamscaleServer teamscaleServer = new TeamscaleServer();

	/**
	 * The configuration necessary to for TIA
	 */
	/* package */ TestImpactConfig testImpactConfig = new TestImpactConfig();

	/**
	 * The port on which the HTTP server should be listening.
	 */
	/* package */ Integer httpServerPort = null;

	/**
	 * Whether classes without coverage should be skipped from the XML report.
	 */
	/* package */ boolean ignoreUncoveredClasses = false;

	/**
	 * The configuration necessary to upload files to an azure file storage
	 */
	/* package */ ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();

	/**
	 * The configuration necessary to upload files to an azure file storage
	 */
	/* package */ AzureFileStorageConfig azureFileStorageConfig = new AzureFileStorageConfig();

	/**
	 * The configuration necessary when used in an SAP NetWeaver Java environment.
	 */
	/* package */ SapNwdiApplications sapNetWeaverJavaApplications = null;

	/**
	 * Whether to obfuscate security related configuration options when dumping them into the log or onto the console or
	 * not.
	 */
	/* package */ boolean obfuscateSecurityRelatedOutputs = true;

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
	 * Remove parts of the API key for security reasons from the options string. String is used for logging purposes.
	 * <p>
	 * Given, for example, "config-file=jacocoagent.properties,teamscale-access-token=unlYgehaYYYhbPAegNWV3WgjOzxkmNHn"
	 * we produce a string with obfuscation: "config-file=jacocoagent.properties,teamscale-access-token=************mNHn"
	 */
	public String getObfuscatedOptionsString() {
		if (getOriginalOptionsString() == null) {
			return "";
		}

		Pattern pattern = Pattern.compile("(.*-access-token=)([^,]+)(.*)");
		Matcher match = pattern.matcher(getOriginalOptionsString());
		if (match.find()) {
			String apiKey = match.group(2);
			String obfuscatedApiKey = String.format("************%s", apiKey.substring(Math.max(0,
					apiKey.length() - 4)));
			return String.format("%s%s%s", match.group(1), obfuscatedApiKey, match.group(3));
		}

		return getOriginalOptionsString();
	}

	/**
	 * Validates the options and returns a validator with all validation errors.
	 */
	/* package */ Validator getValidator() {
		Validator validator = new Validator();

		validateFilePaths(validator);

		if (loggingConfig != null) {
			validateLoggingConfig(validator);
		}

		validator.isFalse(uploadUrl == null && !additionalMetaDataFiles.isEmpty(),
				"You specified additional meta data files to be uploaded but did not configure an upload URL");

		validateTeamscaleUploadConfig(validator);

		validateUploadConfig(validator);

		validateSapNetWeaverConfig(validator);

		validator.isFalse(!useTestwiseCoverageMode() && testImpactConfig.testEnvironmentVariable != null,
				"You use 'test-env' but did not set 'mode' to 'TESTWISE'!");
		if (useTestwiseCoverageMode()) {
			validateTestwiseCoverageConfig(validator);
		}

		return validator;
	}

	private void validateFilePaths(Validator validator) {
		for (File path : classDirectoriesOrZips) {
			validator.isTrue(path.exists(), "Path '" + path + "' does not exist");
			validator.isTrue(path.canRead(), "Path '" + path + "' is not readable");
		}
	}

	private void validateLoggingConfig(Validator validator) {
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

	private void validateTeamscaleUploadConfig(Validator validator) {
		validator.isTrue(teamscaleServer.hasAllRequiredFieldsNull() || teamscaleServer
						.hasAllRequiredFieldsSetExceptProject() || sapNetWeaverJavaApplications != null,
				"You did provide some options prefixed with 'teamscale-', but not all required ones!");

		validator.isFalse(teamscaleServer.hasAllRequiredFieldsSetAndProjectNull() && (teamscaleServer.revision != null
						|| teamscaleServer.commit != null),
				"You tried to provide a commit to upload to directly. This is not possible, since you" +
						" did not provide the 'teamscale-project' Teamscale project to upload to. Please either specify the 'teamscale-project'" +
						" property, or provide the respective projects and commits via all the profiled Jar/War/Ear/...s' " +
						" git.properties files.");

		validator.isTrue(teamscaleServer.revision == null || teamscaleServer.commit == null,
				"'" + TeamscaleConfig.TEAMSCALE_REVISION_OPTION + "' and '" + TeamscaleConfig.TEAMSCALE_REVISION_MANIFEST_JAR_OPTION + "' are incompatible with '" + TeamscaleConfig.TEAMSCALE_COMMIT_OPTION + "' and '" +
						TeamscaleConfig.TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION + "'.");
	}

	private void validateUploadConfig(Validator validator) {
		validator.isTrue((artifactoryConfig.hasAllRequiredFieldsSet() || artifactoryConfig
						.hasAllRequiredFieldsNull()),
				String.format("If you want to upload data to Artifactory you need to provide " +
								"'%s', and an authentication method (either '%s' and '%s' or '%s') ",
						ArtifactoryConfig.ARTIFACTORY_URL_OPTION,
						ArtifactoryConfig.ARTIFACTORY_USER_OPTION, ArtifactoryConfig.ARTIFACTORY_PASSWORD_OPTION,
						ArtifactoryConfig.ARTIFACTORY_API_KEY_OPTION));

		validator.isTrue((azureFileStorageConfig.hasAllRequiredFieldsSet() || azureFileStorageConfig
						.hasAllRequiredFieldsNull()),
				"If you want to upload data to an Azure file storage you need to provide both " +
						"'azure-url' and 'azure-key' ");

		long configuredStores = Stream
				.of(artifactoryConfig.hasAllRequiredFieldsSet(), azureFileStorageConfig.hasAllRequiredFieldsSet(),
						teamscaleServer.hasAllRequiredFieldsSet(), uploadUrl != null).filter(x -> x).count();

		validator.isTrue(configuredStores <= 1, "You cannot configure multiple upload stores, " +
				"such as a Teamscale instance, upload URL, Azure file storage or artifactory");
	}

	private void validateSapNetWeaverConfig(Validator validator) {
		validator.isTrue(sapNetWeaverJavaApplications == null || sapNetWeaverJavaApplications.hasAllRequiredFieldsSet(),
				"You provided an SAP NWDI applications config, but it is empty.");

		validator.isTrue(sapNetWeaverJavaApplications == null || teamscaleServer.hasAllRequiredFieldsSetExceptProject(),
				"You provided an SAP NWDI applications config, but the 'teamscale-' upload options are incomplete.");

		validator.isTrue(sapNetWeaverJavaApplications == null || !teamscaleServer.hasAllRequiredFieldsSet(),
				"You provided an SAP NWDI applications config and a teamscale-project. This is not allowed. " +
						"The project must be specified via sap-nwdi-applications!");
	}

	private void validateTestwiseCoverageConfig(Validator validator) {
		validator.isFalse(
				httpServerPort == null && testImpactConfig.testEnvironmentVariable == null,
				"You use 'mode' 'TESTWISE' but did use neither 'http-server-port' nor 'test-env'!" +
						" One of them is required!");

		validator.isFalse(
				httpServerPort != null && testImpactConfig.testEnvironmentVariable != null,
				"You did set both 'http-server-port' and 'test-env'! Only one of them is allowed!");

		validator.isFalse(uploadUrl != null,
				"'upload-url' option is " +
						"incompatible with Testwise coverage mode!");

		validator.isFalse(testImpactConfig.testwiseCoverageMode == ETestwiseCoverageMode.TEAMSCALE_UPLOAD
						&& !teamscaleServer.hasAllRequiredFieldsSet(),
				"You use 'tia-mode=teamscale-upload' but did not set all required 'teamscale-' fields to facilitate" +
						" a connection to Teamscale!");
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

		if (teamscaleServer.hasAllRequiredFieldsSetAndProjectNull()) {
			logger.info("You did not provide a Teamscale project to upload to directly, so the Agent will try and" +
					" auto-detect it by searching all profiled Jar/War/Ear/... files for git.properties files" +
					" with the 'teamscale.project' field set.");
			return createDelayedMultiProjectTeamscaleUploader(instrumentation);
		}

		if (teamscaleServer.hasAllRequiredFieldsSet()) {
			if (!teamscaleServer.hasCommitOrRevision()) {
				logger.info("You did not provide a commit to upload to directly, so the Agent will try and" +
						" auto-detect it by searching all profiled Jar/War/Ear/... files for a git.properties file.");
				return createDelayedTeamscaleUploader(instrumentation);
			}
			return new TeamscaleUploader(teamscaleServer);
		}

		if (artifactoryConfig.hasAllRequiredFieldsSet()) {
			if (!artifactoryConfig.hasCommitInfo()) {
				logger.info("You did not provide a commit to upload to directly, so the Agent will try and" +
						" auto-detect it by searching all profiled Jar/War/Ear/... files for a git.properties file.");
				return createDelayedArtifactoryUploader(instrumentation);
			}
			return new ArtifactoryUploader(artifactoryConfig,
					additionalMetaDataFiles);
		}

		if (azureFileStorageConfig.hasAllRequiredFieldsSet()) {
			return new AzureFileStorageUploader(azureFileStorageConfig,
					additionalMetaDataFiles);
		}

		if (sapNetWeaverJavaApplications != null && sapNetWeaverJavaApplications
				.hasAllRequiredFieldsSet() && teamscaleServer.hasAllRequiredFieldsSetExceptProject()) {
			logger.info("NWDI configuration detected. The Agent will try and" +
					" auto-detect commit information by searching all profiled Jar/War/Ear/... files.");
			return createNwdiTeamscaleUploader(instrumentation);
		}

		return new LocalDiskUploader();
	}

	private IUploader createDelayedTeamscaleUploader(Instrumentation instrumentation) {
		DelayedUploader<ProjectRevision> uploader = new DelayedUploader<>(
				projectRevision -> {
					if (!StringUtils.isEmpty(projectRevision.getProject()) && !teamscaleServer.project
							.equals(projectRevision.getProject())) {
						logger.warn(
								"Teamscale project '{}' specified in the agent configuration is not the same as the Teamscale project '{}' specified in git.properties file(s). Proceeding to upload to the" +
										" Teamscale project '{}' specified in the agent configuration.",
								teamscaleServer.project, projectRevision.getProject(), teamscaleServer.project);
					}
					teamscaleServer.revision = projectRevision.getRevision();
					return new TeamscaleUploader(teamscaleServer);
				}, outputDirectory);
		GitPropertiesLocator<ProjectRevision> locator = new GitPropertiesLocator<>(uploader,
				GitPropertiesLocatorUtils::getProjectRevisionFromGitProperties);
		instrumentation.addTransformer(new GitPropertiesLocatingTransformer(locator, getLocationIncludeFilter()));
		return uploader;
	}

	private IUploader createDelayedMultiProjectTeamscaleUploader(Instrumentation instrumentation) {
		DelayedTeamscaleMultiProjectUploader uploader = new DelayedTeamscaleMultiProjectUploader((project, revision) ->
				new TeamscaleUploader(teamscaleServer.withProjectAndRevision(project, revision)));
		GitMultiProjectPropertiesLocator locator = new GitMultiProjectPropertiesLocator(uploader);
		instrumentation.addTransformer(new GitPropertiesLocatingTransformer(locator, getLocationIncludeFilter()));
		return uploader;
	}

	private IUploader createDelayedArtifactoryUploader(Instrumentation instrumentation) {
		DelayedUploader<ArtifactoryConfig.CommitInfo> uploader = new DelayedUploader<>(
				commitInfo -> {
					artifactoryConfig.commitInfo = commitInfo;
					return new ArtifactoryUploader(artifactoryConfig, additionalMetaDataFiles);
				}, outputDirectory);
		GitPropertiesLocator<ArtifactoryConfig.CommitInfo> locator = new GitPropertiesLocator<>(uploader,
				(file, isJarFile) -> ArtifactoryConfig.parseGitProperties(
						file, artifactoryConfig.gitPropertiesCommitTimeFormat));
		instrumentation.addTransformer(new GitPropertiesLocatingTransformer(locator, getLocationIncludeFilter()));
		return uploader;
	}

	private IUploader createNwdiTeamscaleUploader(Instrumentation instrumentation) {
		DelayedSapNwdiMultiUploader uploader = new DelayedSapNwdiMultiUploader(
				(commit, application) -> new TeamscaleUploader(
						teamscaleServer.withProjectAndCommit(application.getTeamscaleProject(), commit)));
		instrumentation.addTransformer(new NwdiMarkerClassLocatingTransformer(uploader, getLocationIncludeFilter(),
				sapNetWeaverJavaApplications.getApplications()));
		return uploader;
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
	 * Creates a new temp file with the given prefix, extension and current timestamp and ensures that the parent folder
	 * actually exists.
	 */
	public File createTempFile(String prefix, String extension) throws IOException {
		org.conqat.lib.commons.filesystem.FileSystemUtils.ensureDirectoryExists(outputDirectory.toFile());
		return outputDirectory.resolve(prefix + "-" + LocalDateTime.now().format(DATE_TIME_FORMATTER) + "." + extension)
				.toFile();
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
	public boolean useTestwiseCoverageMode() {
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
		return testImpactConfig.testEnvironmentVariable;
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
	 * @see #obfuscateSecurityRelatedOutputs
	 */
	public boolean shouldObfuscateSecurityRelatedOutputs() {
		return obfuscateSecurityRelatedOutputs;
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

	/** @see TestImpactConfig#testwiseCoverageMode */
	public ETestwiseCoverageMode getTestwiseCoverageMode() {
		return testImpactConfig.testwiseCoverageMode;
	}

	/** @see #ignoreUncoveredClasses */
	public boolean shouldIgnoreUncoveredClasses() {
		return ignoreUncoveredClasses;
	}
}
