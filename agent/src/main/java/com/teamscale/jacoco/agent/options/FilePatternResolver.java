package com.teamscale.jacoco.agent.options;

import com.teamscale.report.util.ILogger;
import org.conqat.lib.commons.collections.Pair;
import org.conqat.lib.commons.filesystem.AntPatternUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/** Helper class to support resolving file paths which may contain ant patterns. */
public class FilePatternResolver {

	/** Stand-in for the question mark operator. */
	private static final String QUESTION_REPLACEMENT = "!@";

	/** Stand-in for the asterisk operator. */
	private static final String ASTERISK_REPLACEMENT = "#@";

	/** Logger. */
	private final ILogger logger;

	public FilePatternResolver(ILogger logger) {
		this.logger = logger;
	}

	/**
	 * Parses the given value as a {@link File}.
	 */
	public File parseFile(String optionName, String value) throws AgentOptionParseException {
		return parsePath(optionName, new File("."), value).toFile();
	}

	/**
	 * Parses the given value as a {@link Path}.
	 */
	public Path parsePath(String optionName, String value) throws AgentOptionParseException {
		return parsePath(optionName, new File("."), value);
	}

	/**
	 * Parses the given value as a {@link Path}.
	 */
	/* package */ Path parsePath(String optionName, File workingDirectory,
								 String value) throws AgentOptionParseException {
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
	private Path parseFileFromPattern(File workingDirectory, String optionName,
									  String value) throws AgentOptionParseException {
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
							" did not match any files in " + basePath.toAbsolutePath());
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
	 * placeholders, and a pattern suffix. We need to replace the pattern characters with stand-ins, because ? and * are
	 * not allowed as path characters on windows.
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

	/**
	 * Returns whether the given path contains artificial pattern characters ({@link #QUESTION_REPLACEMENT}, {@link
	 * #ASTERISK_REPLACEMENT}).
	 */
	private static boolean isPathWithArtificialPattern(String path) {
		return path.contains(QUESTION_REPLACEMENT) || path.contains(ASTERISK_REPLACEMENT);
	}
}
