package org.junit.platform.console.tasks;

import org.junit.platform.console.Logger;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestIdentifierUtils {
	/**
	 * Pattern that matches the classes fully qualified class name in JUnit's uniqueId as first capture group.
	 * The test's uniqueId is something similar to:
	 * [engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[method:testAdd()]
	 * [engine:junit-vintage]/[runner:com.example.project.JUnit4Test]/[test:testAdd(com.example.project.JUnit4Test)]
	 */
	private static final Pattern FULL_CLASS_NAME_PATTERN = Pattern.compile(".*\\[(?:class|runner):([^]]+)\\].*");

	/**
	 * Builds the internal ID which will later be displayed in Teamscale.
	 */
	public static String getTestUniformPath(TestIdentifier testIdentifier, Logger logger) {
		return getFullyQualifiedClassName(testIdentifier, logger) + '/' + testIdentifier.getLegacyReportingName();
	}

	/** Tries to extract the fully qualified class name from the given test identifier. */
	private static String getFullyQualifiedClassName(TestIdentifier testIdentifier, Logger logger) {
		Matcher matcher = FULL_CLASS_NAME_PATTERN.matcher(testIdentifier.getUniqueId());
		String fullClassName = "unknown-class";
		if (!matcher.matches()) {
			logger.error("Unable to find class name for " + testIdentifier.getUniqueId());

			MethodSource ms = (MethodSource) testIdentifier.getSource().orElse(null);
			if (ms != null) {
				fullClassName = ms.getClassName();
			}
		} else {
			fullClassName = matcher.group(1);
		}
		return fullClassName.replace('.', '/');
	}
}
