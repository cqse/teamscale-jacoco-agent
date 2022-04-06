/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.options;

import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.commandline.Validator;
import com.teamscale.jacoco.agent.options.sapnwdi.SapNwdiApplications;
import com.teamscale.jacoco.agent.testimpact.TestImpactConfig;
import com.teamscale.jacoco.agent.upload.artifactory.ArtifactoryConfig;
import com.teamscale.jacoco.agent.upload.azure.AzureFileStorageConfig;
import com.teamscale.jacoco.agent.upload.teamscale.TeamscaleConfig;
import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.util.ILogger;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.collections.Pair;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Parses agent command line options.
 */
public class AgentOptionsParser {

	/** The name of the option for providing the logging config. */
	public static final String LOGGING_CONFIG_OPTION = "logging-config";

	/** The name of the option for providing the config file. */
	public static final String CONFIG_FILE_OPTION = "config-file";

	/** Character which starts a comment in the config file. */
	private static final String COMMENT_PREFIX = "#";

	private final ILogger logger;
	private final FilePatternResolver filePatternResolver;
	private final TeamscaleConfig teamscaleConfig;

	public AgentOptionsParser(ILogger logger) {
		this.logger = logger;
		this.filePatternResolver = new FilePatternResolver(logger);
		this.teamscaleConfig = new TeamscaleConfig(logger, filePatternResolver);
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
		logger.debug("Parsing options: " + optionsString);
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
		Pair<String, String> keyAndValue = parseOption(optionPart);
		String key = keyAndValue.getFirst();
		String value = keyAndValue.getSecond();
		if (key.startsWith("jacoco-")) {
			options.additionalJacocoOptions.add(key.substring(7), value);
			return;
		}
		if (key.startsWith("teamscale-") && teamscaleConfig.handleTeamscaleOptions(options.teamscaleServer, key,
				value)) {
			return;
		}
		if (TestImpactConfig.handleTiaOptions(options.testImpactConfig, key, value)) {
			return;
		}
		if (key.startsWith("artifactory-") && ArtifactoryConfig
				.handleArtifactoryOptions(options.artifactoryConfig, filePatternResolver, key, value)) {
			return;
		}
		if (key.startsWith("azure-") && AzureFileStorageConfig
				.handleAzureFileStorageOptions(options.azureFileStorageConfig, key,
						value)) {
			return;
		}
		if (handleAgentOptions(options, key, value)) {
			return;
		}
		throw new AgentOptionParseException("Unknown option: " + key);
	}

	private Pair<String, String> parseOption(String optionPart) throws AgentOptionParseException {
		String[] keyAndValue = optionPart.split("=", 2);
		if (keyAndValue.length < 2) {
			throw new AgentOptionParseException("Got an option without any value: " + optionPart);
		}

		String key = keyAndValue[0].toLowerCase();
		String value = keyAndValue[1];

		// Remove quotes, which may be used to pass arguments with spaces via
		// the command line
		if (value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length() - 1);
		}
		return new Pair<>(key, value);
	}

	/**
	 * Handles all common command line options for the agent.
	 *
	 * @return true if it has successfully processed the given option.
	 */
	private boolean handleAgentOptions(AgentOptions options, String key, String value)
			throws AgentOptionParseException {
		switch (key) {
			case CONFIG_FILE_OPTION:
				readConfigFromFile(options, filePatternResolver.parsePath(key, value).toFile());
				return true;
			case LOGGING_CONFIG_OPTION:
				options.loggingConfig = filePatternResolver.parsePath(key, value);
				return true;
			case "interval":
				options.dumpIntervalInMinutes = parseInt(key, value);
				return true;
			case "validate-ssl":
				options.validateSsl = Boolean.parseBoolean(value);
				return true;
			case "out":
				options.setParentOutputDirectory(filePatternResolver.parsePath(key, value));
				return true;
			case "upload-url":
				options.uploadUrl = parseUrl(key, value);
				return true;
			case "upload-metadata":
				try {
					options.additionalMetaDataFiles = CollectionUtils.map(splitMultiOptionValue(value), Paths::get);
				} catch (InvalidPathException e) {
					throw new AgentOptionParseException("Invalid path given for option 'upload-metadata'", e);
				}
				return true;
			case "duplicates":
				options.duplicateClassFileBehavior = parseEnumValue(key, value, EDuplicateClassFileBehavior.class);
				return true;
			case "ignore-uncovered-classes":
				options.ignoreUncoveredClasses = Boolean.parseBoolean(value);
				return true;
			case "obfuscate-security-related-outputs":
				options.obfuscateSecurityRelatedOutputs = Boolean.parseBoolean(value);
				return true;
			case "dump-on-exit":
				options.shouldDumpOnExit = Boolean.parseBoolean(value);
				return true;
			case "mode":
				options.mode = parseEnumValue(key, value, EMode.class);
				return true;
			case "includes":
				options.jacocoIncludes = value.replaceAll(";", ":");
				return true;
			case "excludes":
				options.jacocoExcludes = value.replaceAll(";", ":") + ":" + AgentOptions.DEFAULT_EXCLUDES;
				return true;
			case "class-dir":
				List<String> list = splitMultiOptionValue(value);
				options.classDirectoriesOrZips = ClasspathUtils.resolveClasspathTextFiles(key, filePatternResolver,
						list);
				return true;
			case "http-server-port":
				options.httpServerPort = parseInt(key, value);
				return true;
			case "sap-nwdi-applications":
				options.sapNetWeaverJavaApplications = SapNwdiApplications.parseApplications(value);
				return true;
			default:
				return false;
		}
	}

	/**
	 * Parses the given value as an enum constant case-insensitively and converts "-" to "_".
	 */
	public static <T extends Enum<T>> T parseEnumValue(String key, String value, Class<T> enumClass)
			throws AgentOptionParseException {
		try {
			return Enum.valueOf(enumClass, value.toUpperCase().replaceAll("-", "_"));
		} catch (IllegalArgumentException e) {
			String validValues = Arrays.stream(enumClass.getEnumConstants()).map(T::name).collect(joining(", "));
			throw new AgentOptionParseException("Invalid value for option `" + key + "`. Valid values: " + validValues,
					e);
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

	private int parseInt(String key, String value) throws AgentOptionParseException {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new AgentOptionParseException("Invalid non-numeric value for option `" + key + "`: " + value);
		}
	}

	/**
	 * Parses the given value as a URL.
	 */
	public static HttpUrl parseUrl(String key, String value) throws AgentOptionParseException {
		if (!value.endsWith("/")) {
			value += "/";
		}

		// default to HTTP if no scheme is given and port is not 443, default to HTTPS if no scheme is given AND port is 443
		if (!value.startsWith("http://") && !value.startsWith("https://")) {
			HttpUrl url = getUrl(key, "http://" + value);
			if (url.port() == 443) {
				value = "https://" + value;
			} else {
				value = "http://" + value;
			}
		}

		return getUrl(key, value);
	}

	private static HttpUrl getUrl(String key, String value) throws AgentOptionParseException {
		HttpUrl url = HttpUrl.parse(value);
		if (url == null) {
			throw new AgentOptionParseException("Invalid URL given for option '" + key + "'");
		}
		return url;
	}

	/**
	 * Splits the given value at semicolons.
	 */
	private static List<String> splitMultiOptionValue(String value) {
		return Arrays.asList(value.split(";"));
	}
}
