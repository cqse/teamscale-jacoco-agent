/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.options;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.commandline.Validator;
import com.teamscale.jacoco.agent.git_properties.GitPropertiesLocator;
import com.teamscale.jacoco.agent.git_properties.InvalidGitPropertiesException;
import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.util.BashFileSkippingInputStream;
import com.teamscale.report.util.ILogger;

import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import okhttp3.HttpUrl;

/**
 * Parses agent command line options.
 */
public class AgentOptionsParser {

	/** Character which starts a comment in the config file. */
	private static final String COMMENT_PREFIX = "#";

	private final ILogger logger;
	private final FilePatternResolver filePatternResolver;

	public AgentOptionsParser(ILogger logger) {
		this.logger = logger;
		this.filePatternResolver = new FilePatternResolver(logger);
	}

	/**
	 * Parses the given command-line options.
	 */
	public static AgentOptions parse(String optionsString, ILogger logger) throws AgentOptionParseException {
		return new AgentOptionsParser(logger).parse(optionsString);
	}

	/**
	 * Parses the given command-line options.
	 */
	/* package */ AgentOptions parse(String optionsString) throws AgentOptionParseException {
		if (optionsString == null) {
			optionsString = "";
		}

		AgentOptions options = new AgentOptions();
		options.originalOptionsString = optionsString;

		if (!StringUtils.isEmpty(optionsString)) {
			String[] optionParts = optionsString.split(",");
			for (String optionPart : optionParts) {
				handleOption(options, optionPart);
			}
		}

		Validator validator = options.getValidator();
		if (!validator.isValid()) {
			throw new AgentOptionParseException("Invalid options given: " + validator.getErrorMessage());
		}
		return options;
	}

	/**
	 * Parses and stores the given option in the format <code>key=value</code>.
	 */
	private void handleOption(AgentOptions options, String optionPart) throws AgentOptionParseException {
		String[] keyAndValue = optionPart.split("=", 2);
		if (keyAndValue.length < 2) {
			throw new AgentOptionParseException("Got an option without any value: " + optionPart);
		}

		String key = keyAndValue[0].toLowerCase();
		String value = keyAndValue[1];

		// Remove quotes, which may be used to pass arguments with spaces via the command line
		if (value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length() - 1);
		}

		if (key.startsWith("jacoco-")) {
			options.additionalJacocoOptions.add(key.substring(7), value);
			return;
		} else if (key.startsWith("teamscale-") && handleTeamscaleOptions(options, key, value)) {
			return;
		} else if (handleHttpServerOptions(options, key, value)) {
			return;
		} else if (key.startsWith("azure-") && handleAzureFileStorageOptions(options, key, value)) {
			return;
		} else if (handleAgentOptions(options, key, value)) {
			return;
		}
		throw new AgentOptionParseException("Unknown option: " + key);
	}

	/**
	 * Handles all command line options for the agent without special prefix.
	 *
	 * @return true if it has successfully process the given option.
	 */
	private boolean handleAgentOptions(AgentOptions options, String key,
									   String value) throws AgentOptionParseException {
		switch (key) {
			case "config-file":
				readConfigFromFile(options, filePatternResolver.parsePath(key, value).toFile());
				return true;
			case "logging-config":
				options.loggingConfig = filePatternResolver.parsePath(key, value);
				return true;
			case "interval":
				try {
					options.dumpIntervalInMinutes = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new AgentOptionParseException("Non-numeric value given for option 'interval'");
				}
				return true;
			case "validate-ssl":
				options.validateSsl = Boolean.parseBoolean(value);
				return true;
			case "out":
				options.setParentOutputDirectory(filePatternResolver.parsePath(key, value));
				return true;
			case "upload-url":
				options.uploadUrl = parseUrl(value);
				if (options.uploadUrl == null) {
					throw new AgentOptionParseException("Invalid URL given for option 'upload-url'");
				}
				return true;
			case "upload-metadata":
				try {
					options.additionalMetaDataFiles = CollectionUtils.map(splitMultiOptionValue(value), Paths::get);
				} catch (InvalidPathException e) {
					throw new AgentOptionParseException("Invalid path given for option 'upload-metadata'", e);
				}
				return true;
			case "duplicates":
				options.duplicateClassFileBehavior = EDuplicateClassFileBehavior.valueOf(value.toUpperCase());
			case "ignore-uncovered-classes":
				options.ignoreUncoveredClasses = Boolean.parseBoolean(value);
			case "dump-on-exit":
				options.shouldDumpOnExit = Boolean.parseBoolean(value);
				return true;
			case "mode":
				options.mode = EMode.valueOf(value.toUpperCase());
				return true;
			case "includes":
				options.jacocoIncludes = value.replaceAll(";", ":");
				return true;
			case "excludes":
				options.jacocoExcludes = value.replaceAll(";", ":");
				return true;
			case "class-dir":
				List<String> list = splitMultiOptionValue(value);
				options.classDirectoriesOrZips = ClasspathUtils
						.resolveClasspathTextFiles(key, filePatternResolver, list);
				return true;
			default:
				return false;
		}
	}

	/**
	 * Reads configuration parameters from the given file. The expected format is basically the same as for the command
	 * line, but line breaks are also considered as separators. e.g. class-dir=out # Some comment includes=test.*
	 * excludes=third.party.*
	 */
	private void readConfigFromFile(AgentOptions options, File configFile) throws AgentOptionParseException {
		try {
			List<String> configFileKeyValues = FileSystemUtils.readLinesUTF8(configFile);
			for (String optionKeyValue : configFileKeyValues) {
				String trimmedOption = optionKeyValue.trim();
				if (trimmedOption.isEmpty() || trimmedOption.startsWith(COMMENT_PREFIX)) {
					continue;
				}
				handleOption(options, optionKeyValue);
			}
		} catch (FileNotFoundException e) {
			throw new AgentOptionParseException(
					"File " + configFile.getAbsolutePath() + " given for option 'config-file' not found", e);
		} catch (IOException e) {
			throw new AgentOptionParseException(
					"An error occurred while reading the config file " + configFile.getAbsolutePath(), e);
		}
	}

	/**
	 * Handles all command line options prefixed with "teamscale-".
	 *
	 * @return true if it has successfully process the given option.
	 */
	private boolean handleTeamscaleOptions(AgentOptions options, String key,
										   String value) throws AgentOptionParseException {
		switch (key) {
			case "teamscale-server-url":
				options.teamscaleServer.url = parseUrl(value);
				if (options.teamscaleServer.url == null) {
					throw new AgentOptionParseException(
							"Invalid URL " + value + " given for option 'teamscale-server-url'");
				}
				return true;
			case "teamscale-project":
				options.teamscaleServer.project = value;
				return true;
			case "teamscale-user":
				options.teamscaleServer.userName = value;
				return true;
			case "teamscale-access-token":
				options.teamscaleServer.userAccessToken = value;
				return true;
			case "teamscale-partition":
				options.teamscaleServer.partition = value;
				return true;
			case AgentOptions.TEAMSCALE_COMMIT_OPTION:
				options.teamscaleServer.commit = parseCommit(value);
				return true;
			case AgentOptions.TEAMSCALE_COMMIT_MANIFEST_JAR_OPTION:
				options.teamscaleServer.commit = getCommitFromManifest(
						filePatternResolver.parsePath(key, value).toFile());
				return true;
			case AgentOptions.TEAMSCALE_GIT_PROPERTIES_JAR_OPTION:
				options.teamscaleServer.revision = parseGitProperties(key, value);
				return true;
			case "teamscale-message":
				options.teamscaleServer.message = value;
				return true;
			case AgentOptions.TEAMSCALE_REVISION_OPTION:
				options.teamscaleServer.revision = value;
				return true;
			case "teamscale-testwise-upload":
				if (Boolean.parseBoolean(value)) {
					options.testWiseCoverageMode = ETestWiseCoverageMode.TEAMSCALE_REPORT;
				}
				return true;
			default:
				return false;
		}
	}

	private String parseGitProperties(String key, String value) throws AgentOptionParseException {
		File jarFile = filePatternResolver.parsePath(key, value).toFile();
		try {
			String commit = GitPropertiesLocator.getCommitFromGitProperties(jarFile);
			if (commit == null) {
				throw new AgentOptionParseException("Could not locate a git.properties file in " + jarFile.toString());
			}
			return commit;
		} catch (IOException | InvalidGitPropertiesException e) {
			throw new AgentOptionParseException("Could not locate a valid git.properties file in " + jarFile.toString(),
					e);
		}
	}

	/**
	 * Handles all command-line options prefixed with 'azure-'
	 *
	 * @return true if it has successfully process the given option.
	 */
	private boolean handleAzureFileStorageOptions(AgentOptions options, String key, String value)
			throws AgentOptionParseException {
		switch (key) {
			case "azure-url":
				options.azureFileStorageConfig.url = parseUrl(value);
				if (options.azureFileStorageConfig.url == null) {
					throw new AgentOptionParseException("Invalid URL given for option 'upload-azure-url'");
				}
				return true;
			case "azure-key":
				options.azureFileStorageConfig.accessKey = value;
				return true;
			default:
				return false;
		}
	}

	/**
	 * Reads `Branch` and `Timestamp` entries from the given jar/war file's manifest and builds a commit descriptor out
	 * of it.
	 */
	private CommitDescriptor getCommitFromManifest(File jarFile) throws AgentOptionParseException {
		try (JarInputStream jarStream = new JarInputStream(
				new BashFileSkippingInputStream(new FileInputStream(jarFile)))) {
			Manifest manifest = jarStream.getManifest();
			if (manifest == null) {
				throw new AgentOptionParseException(
						"Unable to read manifest from " + jarFile + ". Maybe the manifest is corrupt?");
			}
			String branch = manifest.getMainAttributes().getValue("Branch");
			String timestamp = manifest.getMainAttributes().getValue("Timestamp");
			if (StringUtils.isEmpty(branch)) {
				throw new AgentOptionParseException("No entry 'Branch' in MANIFEST");
			} else if (StringUtils.isEmpty(timestamp)) {
				throw new AgentOptionParseException("No entry 'Timestamp' in MANIFEST");
			}
			logger.debug("Found commit " + branch + ":" + timestamp + " in file " + jarFile);
			return new CommitDescriptor(branch, timestamp);
		} catch (IOException e) {
			throw new AgentOptionParseException("Reading jar " + jarFile.getAbsolutePath() + " for obtaining commit " +
					"descriptor from MANIFEST failed", e);
		}
	}

	/**
	 * Handles all command line options prefixed with "http-server-".
	 *
	 * @return true if it has successfully process the given option.
	 */
	private boolean handleHttpServerOptions(AgentOptions options, String key,
											String value) throws AgentOptionParseException {
		switch (key) {
			case "test-env":
				options.testEnvironmentVariable = value;
				return true;
			case "http-server-port":
				try {
					options.httpServerPort = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new AgentOptionParseException(
							"Invalid port number " + value + " given for option 'http-server-port'");
				}
				return true;
			case "coverage-via-http":
				if (Boolean.parseBoolean(value)) {
					options.testWiseCoverageMode = ETestWiseCoverageMode.HTTP;
				}
				return true;
			default:
				return false;
		}
	}


	/**
	 * Parses the given value as a URL or returns <code>null</code> if that fails.
	 */
	private static HttpUrl parseUrl(String value) {
		// default to HTTP if no scheme is given
		if (!value.startsWith("http://") && !value.startsWith("https://")) {
			value = "http://" + value;
		}

		if (!value.endsWith("/")) {
			value += "/";
		}

		return HttpUrl.parse(value);
	}


	/**
	 * Parses the the string representation of a commit to a  {@link CommitDescriptor} object.
	 * <p>
	 * The expected format is "branch:timestamp".
	 */
	private static CommitDescriptor parseCommit(String commit) throws AgentOptionParseException {
		String[] split = commit.split(":");
		if (split.length != 2) {
			throw new AgentOptionParseException("Invalid commit given " + commit);
		}
		return new CommitDescriptor(split[0], split[1]);
	}

	/**
	 * Splits the given value at semicolons.
	 */
	private static List<String> splitMultiOptionValue(String value) {
		return Arrays.asList(value.split(";"));
	}
}
