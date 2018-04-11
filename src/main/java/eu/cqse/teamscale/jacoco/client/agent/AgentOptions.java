/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.agent;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import eu.cqse.teamscale.jacoco.client.commandline.Validator;

/**
 * Parses agent command line options.
 */
public class AgentOptions {

	/** Thrown if option parsing fails. */
	public static class AgentOptionParseException extends Exception {

		/** Serialization ID. */
		private static final long serialVersionUID = 1L;

		/** Constructor. */
		public AgentOptionParseException(String message) {
			super(message);
		}

		/** Constructor. */
		public AgentOptionParseException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	/** The directories and/or zips that contain all class files being profiled. */
	private List<File> classDirectoriesOrZips = new ArrayList<>();

	/**
	 * Ant-style include patterns to apply during JaCoCo's traversal of class files.
	 */
	private List<String> locationIncludeFilters = new ArrayList<>();

	/**
	 * Ant-style exclude patterns to apply during JaCoCo's traversal of class files.
	 */
	private List<String> locationExcludeFilters = new ArrayList<>();

	/** The directory to write the XML traces to. */
	private Path outputDir = null;

	/** The interval in minutes for dumping XML data. */
	private int dumpIntervalInMinutes = 60;

	/** Whether to ignore duplicate, non-identical class files. */
	private boolean shouldIgnoreDuplicateClassFiles = false;

	/** Include patterns to pass on to JaCoCo. */
	private String jacocoIncludes = null;

	/** Exclude patterns to pass on to JaCoCo. */
	private String jacocoExcludes = null;

	/** Parses the given command-line options. */
	public AgentOptions(String options) throws AgentOptionParseException {
		if (options.isEmpty()) {
			throw new AgentOptionParseException(
					"No agent options given. You must at least provide an output directory (out)"
							+ " and a classes directory (class-dir)");
		}

		String[] optionParts = options.split(",");
		for (String optionPart : optionParts) {
			handleOption(optionPart);
		}

		validate();
	}

	/**
	 * Validates the options and throws an exception if they're not valid.
	 */
	private void validate() throws AgentOptionParseException {
		Validator validator = new Validator();

		validator.isFalse(getClassDirectoriesOrZips().isEmpty(),
				"You must specify at least one directory or zip that contains class files");
		for (File path : classDirectoriesOrZips) {
			validator.isTrue(path.exists(), "Path '" + path + "' does not exist");
			validator.isTrue(path.canRead(), "Path '" + path + "' is not readable");
		}

		validator.ensure(() -> {
			CCSMAssert.isNotNull(outputDir, "You must specify an output directory");
			FileSystemUtils.ensureDirectoryExists(outputDir.toFile());
			CCSMAssert.isTrue(outputDir.toFile().canWrite(), "Path '" + outputDir + "' is not writable");
		});

		if (!validator.isValid()) {
			throw new AgentOptionParseException("Invalid options given: " + validator.getErrorMessage());
		}
	}

	/**
	 * Parses and stores the given option in the format <code>key=value</code>.
	 */
	private void handleOption(String optionPart) throws AgentOptionParseException {
		String[] keyAndValue = optionPart.split("=", 2);
		if (keyAndValue.length < 2) {
			throw new AgentOptionParseException("Got an option without any value: " + optionPart);
		}

		String key = keyAndValue[0];
		String value = keyAndValue[1];

		switch (key.toLowerCase()) {
		case "interval":
			dumpIntervalInMinutes = Integer.parseInt(value);
			break;
		case "out":
			outputDir = Paths.get(value);
			break;
		case "ignore-duplicates":
			shouldIgnoreDuplicateClassFiles = Boolean.parseBoolean(value);
			break;
		case "include":
			locationIncludeFilters = splitMultiOptionValue(value);
			break;
		case "exclude":
			locationExcludeFilters = splitMultiOptionValue(value);
			break;
		case "class-dir":
			classDirectoriesOrZips = CollectionUtils.map(splitMultiOptionValue(value), File::new);
			break;
		case "jacoco-include":
			jacocoIncludes = value;
			break;
		case "jacoco-exclude":
			jacocoExcludes = value;
			break;
		default:
			throw new AgentOptionParseException("Unknown option: " + key);
		}
	}

	/** Splits the given value at colons. */
	private static List<String> splitMultiOptionValue(String value) {
		return Arrays.asList(value.split(":"));
	}

	/** Returns the options to pass to the JaCoCo agent. */
	public String createJacocoAgentOptions() {
		StringBuilder builder = new StringBuilder("output=none");
		if (jacocoIncludes != null) {
			builder.append(",includes=").append(jacocoIncludes);
		}
		if (jacocoExcludes != null) {
			builder.append(",excludes=").append(jacocoExcludes);
		}
		return builder.toString();
	}

	/** @see #classDirectoriesOrZips */
	public List<File> getClassDirectoriesOrZips() {
		return classDirectoriesOrZips;
	}

	/** @see #locationIncludeFilters */
	public List<String> getLocationIncludeFilters() {
		return locationIncludeFilters;
	}

	/** @see #locationExcludeFilters */
	public List<String> getLocationExcludeFilters() {
		return locationExcludeFilters;
	}

	/** @see #outputDir */
	public Path getOutputDir() {
		return outputDir;
	}

	/** @see #dumpIntervalInMinutes */
	public int getDumpIntervalInMinutes() {
		return dumpIntervalInMinutes;
	}

	/** @see #shouldIgnoreDuplicateClassFiles */
	public boolean isShouldIgnoreDuplicateClassFiles() {
		return shouldIgnoreDuplicateClassFiles;
	}

}
