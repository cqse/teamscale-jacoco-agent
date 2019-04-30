/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent;

import com.teamscale.client.StringUtils;
import com.teamscale.jacoco.agent.commandline.Validator;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.report.util.ILogger;
import okhttp3.HttpUrl;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.collections.Pair;
import org.conqat.lib.commons.filesystem.AntPatternUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Parses agent command line options.
 */
public class AgentOptionsParser {

	/** Character which starts a comment in the config file. */
	private static final String COMMENT_PREFIX = "#";

	/** Stand-in for the question mark operator. */
	private static final String QUESTION_REPLACEMENT = "!@";

	/** Stand-in for the asterisk operator. */
	private static final String ASTERISK_REPLACEMENT = "#@";

	/** Logger. */
	private final ILogger logger;

	public AgentOptionsParser(ILogger logger) {
		this.logger = logger;
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
		if (StringUtils.isEmpty(optionsString)) {
			throw new AgentOptionParseException(
					"No agent options given. You must at least provide an output directory (out)"
							+ " and a classes directory (class-dir)");
		}

		AgentOptions options = new AgentOptions();
		options.originalOptionsString = optionsString;

		String[] optionParts = optionsString.split(",");
		for (String optionPart : optionParts) {
			handleOption(options, optionPart);
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
	private boolean handleAgentOptions(AgentOptions options, String key, String value) throws AgentOptionParseException {
		switch (key) {
			case "config-file":
				readConfigFromFile(options, parseFile(key, value));
				return true;
			case "logging-config":
				options.loggingConfig = parsePath(key, value);
				return true;
			case "interval":
				try {
					options.dumpIntervalInMinutes = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new AgentOptionParseException("Non-numeric value given for option 'interval'");
				}
				return true;
			case "out":
				options.outputDirectory = parsePath(key, value);
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
			case "ignore-duplicates":
				options.shouldIgnoreDuplicateClassFiles = Boolean.parseBoolean(value);
				return true;
			case "includes":
				options.jacocoIncludes = value.replaceAll(";", ":");
				return true;
			case "excludes":
				options.jacocoExcludes = value.replaceAll(";", ":");
				return true;
			case "class-dir":
				options.classDirectoriesOrZips = CollectionUtils
						.mapWithException(splitMultiOptionValue(value), singleValue -> parseFile(key, singleValue));
				return true;
			default:
				return false;
		}
	}

	/**
	 * Reads configuration parameters from the given file.
	 * The expected format is basically the same as for the command line, but line breaks are also considered as
	 * separators.
	 * e.g.
	 * class-dir=out
	 * # Some comment
	 * includes=test.*
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
					"File " + configFile.getAbsolutePath() + " given for option 'config-file' not found!", e);
		} catch (IOException e) {
			throw new AgentOptionParseException(
					"An error occurred while reading the config file " + configFile.getAbsolutePath() + "!", e);
		}
	}

	/**
	 * Handles all command line options prefixed with "teamscale-".
	 *
	 * @return true if it has successfully process the given option.
	 */
	private boolean handleTeamscaleOptions(AgentOptions options, String key, String value) throws AgentOptionParseException {
		switch (key) {
			case "teamscale-server-url":
				options.teamscaleServer.url = parseUrl(value);
				if (options.teamscaleServer.url == null) {
					throw new AgentOptionParseException(
							"Invalid URL " + value + " given for option 'teamscale-server-url'!");
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
			case "teamscale-commit":
				options.teamscaleServer.commit = parseCommit(value);
				return true;
			case "teamscale-commit-manifest-jar":
				options.teamscaleServer.commit = getCommitFromManifest(parseFile(key, value));
				return true;
			case "teamscale-message":
				options.teamscaleServer.message = value;
				return true;
			default:
				return false;
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
	 * Reads `Branch` and `Timestamp` entries from the given jar/war file and
	 * builds a commit descriptor out of it.
	 */
	private CommitDescriptor getCommitFromManifest(File jarFile) throws AgentOptionParseException {
		try (JarInputStream jarStream = new JarInputStream(new FileInputStream(jarFile))) {
			Manifest manifest = jarStream.getManifest();
			String branch = manifest.getMainAttributes().getValue("Branch");
			String timestamp = manifest.getMainAttributes().getValue("Timestamp");
			if (StringUtils.isEmpty(branch)) {
				throw new AgentOptionParseException("No entry 'Branch' in MANIFEST!");
			} else if (StringUtils.isEmpty(timestamp)) {
				throw new AgentOptionParseException("No entry 'Timestamp' in MANIFEST!");
			}
			logger.debug("Found commit " + branch + ":" + timestamp + " in file " + jarFile);
			return new CommitDescriptor(branch, timestamp);
		} catch (IOException e) {
			throw new AgentOptionParseException("Reading jar " + jarFile.getAbsolutePath() + " failed!", e);
		}
	}

	/**
	 * Handles all command line options prefixed with "http-server-".
	 *
	 * @return true if it has successfully process the given option.
	 */
	private boolean handleHttpServerOptions(AgentOptions options, String key, String value) throws AgentOptionParseException {
		switch (key) {
			case "test-env":
				options.testEnvironmentVariable = value;
				return true;
			case "http-server-port":
				try {
					options.httpServerPort = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new AgentOptionParseException(
							"Invalid port number " + value + " given for option 'http-server-port'!");
				}
				return true;
			default:
				return false;
		}
	}

	/**
	 * Parses the given value as a {@link File}.
	 */
	/* package */ File parseFile(String optionName, String value) throws AgentOptionParseException {
		return parsePath(optionName, new File("."), value).toFile();
	}

	/**
	 * Parses the given value as a {@link Path}.
	 */
	/* package */ Path parsePath(String optionName, String value) throws AgentOptionParseException {
		return parsePath(optionName, new File("."), value);
	}

	/**
	 * Parses the given value as a {@link File}.
	 */
	/* package */ File parseFile(String optionName, File workingDirectory, String value) throws AgentOptionParseException {
		return parsePath(optionName, workingDirectory, value).toFile();
	}

	/**
	 * Parses the given value as a {@link Path}.
	 */
	/* package */ Path parsePath(String optionName, File workingDirectory, String value) throws AgentOptionParseException {
		if (isPathWithPattern(value)) {
			return parseFileFromPattern(workingDirectory, optionName, value);
		}
		try {
			return workingDirectory.toPath().resolve(Paths.get(value));
		} catch (InvalidPathException e) {
			throw new AgentOptionParseException("Invalid path given for option " + optionName + ": " + value, e);
		}
	}

	/** Parses the value as a ant pattern to a file or directory. */
	private Path parseFileFromPattern(File workingDirectory, String optionName, String value) throws AgentOptionParseException {
		Pair<String, String> baseDirAndPattern = splitIntoBaseDirAndPattern(value);
		String baseDir = baseDirAndPattern.getFirst();
		String pattern = baseDirAndPattern.getSecond();

		File workingDir = workingDirectory.getAbsoluteFile();
		Path basePath = workingDir.toPath().resolve(baseDir).normalize().toAbsolutePath();

		Pattern pathMatcher = AntPatternUtils.convertPattern(pattern, false);
		Predicate<Path> filter = path -> pathMatcher
				.matcher(FileSystemUtils.normalizeSeparators(basePath.relativize(path).toString())).matches();

		List<Path> matchingPaths;
		try {
			matchingPaths = Files.walk(basePath).filter(filter).sorted().collect(toList());
		} catch (IOException e) {
			throw new AgentOptionParseException(
					"Could not recursively list files in directory " + basePath + " in order to resolve pattern " + pattern + " given for option " + optionName,
					e);
		}

		if (matchingPaths.isEmpty()) {
			throw new AgentOptionParseException(
					"Invalid path given for option " + optionName + ": " + value + ". The pattern " + pattern +
							" did not match any files in " + basePath.toAbsolutePath() + "!");
		} else if (matchingPaths.size() > 1) {
			logger.warn(
					"Multiple files match the pattern " + pattern + " in " + basePath
							.toString() + " for option " + optionName + "! " +
							"The first one is used, but consider to adjust the " +
							"pattern to match only one file. Candidates are: " + matchingPaths.stream()
							.map(basePath::relativize).map(Path::toString).collect(joining(", ")));
		}
		Path path = matchingPaths.get(0).normalize();
		logger.info("Using file " + path + " for option " + optionName);

		return path;
	}

	/**
	 * Splits the path into a base dir, a the directory-prefix of the path that does not contain any ? or *
	 * placeholders, and a pattern suffix.
	 * We need to replace the pattern characters with stand-ins, because ? and * are not allowed as path characters on windows.
	 */
	private Pair<String, String> splitIntoBaseDirAndPattern(String value) {
		String pathWithArtificialPattern = value.replace("?", QUESTION_REPLACEMENT).replace("*", ASTERISK_REPLACEMENT);
		Path pathWithPattern = Paths.get(pathWithArtificialPattern);
		Path baseDir = pathWithPattern;
		while (isPathWithArtificialPattern(baseDir.toString())) {
			baseDir = baseDir.getParent();
			if (baseDir == null) {
				return new Pair<>("", value);
			}
		}
		String pattern = baseDir.relativize(pathWithPattern).toString().replace(QUESTION_REPLACEMENT, "?")
				.replace(ASTERISK_REPLACEMENT, "*");
		return new Pair<>(baseDir.toString(), pattern);
	}

	/** Returns whether the given path contains ant pattern characters (?,*). */
	private static boolean isPathWithPattern(String path) {
		return path.contains("?") || path.contains("*");
	}

	/** Returns whether the given path contains artificial pattern characters ({@link #QUESTION_REPLACEMENT}, {@link #ASTERISK_REPLACEMENT}). */
	private static boolean isPathWithArtificialPattern(String path) {
		return path.contains(QUESTION_REPLACEMENT) || path.contains(ASTERISK_REPLACEMENT);
	}

	/**
	 * Parses the given value as a URL or returns <code>null</code> if that fails.
	 */
	private static HttpUrl parseUrl(String value) {
		// default to HTTP if no scheme is given
		if (!value.startsWith("http://") && !value.startsWith("https://")) {
			value = "http://" + value;
		}
		
		if(!value.endsWith("/")) {
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
