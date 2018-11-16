package com.teamscale.report.util;

import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.runtime.WildcardMatcher;
import org.jacoco.report.JavaNames;

import java.util.function.Predicate;

/***
 * Tests given class file paths against call name patterns.
 * E.g. "/some/file/path/test.jar@my/package/Test.class" matches "my/package/*" or "my/package/Test"
 */
public class ClasspathWildcardIncludeFilter implements Predicate<String> {

	/**
	 * Include patterns to apply during JaCoCo's traversal of class files. If null
	 * then everything is included.
	 */
	private WildcardMatcher locationIncludeFilters = null;

	/**
	 * Exclude patterns to apply during JaCoCo's traversal of class files. If null
	 * then nothing is excluded.
	 */
	private WildcardMatcher locationExcludeFilters = null;

	/**
	 * Constructor.
	 *
	 * @param locationIncludeFilters Colon separated list of wildcard include patterns or null for no includes.
	 * @param locationExcludeFilters Colon separated list of wildcard exclude patterns or null for no excludes.
	 */
	public ClasspathWildcardIncludeFilter(String locationIncludeFilters, String locationExcludeFilters) {
		if (locationIncludeFilters != null) {
			this.locationIncludeFilters = new WildcardMatcher(locationIncludeFilters);
		}
		if (locationExcludeFilters != null) {
			this.locationExcludeFilters = new WildcardMatcher(locationExcludeFilters);
		}
	}

	@Override
	public boolean test(String path) {
		String className = getClassName(path);
		// first check includes
		if (locationIncludeFilters != null && !locationIncludeFilters.matches(className)) {
			return false;
		}
		// if they match, check excludes
		return locationExcludeFilters == null || !locationExcludeFilters.matches(className);
	}

	/**
	 * Returns the normalized class name of the given class file's path.
	 */
	/* package */ static String getClassName(String path) {
		String[] parts = FileSystemUtils.normalizeSeparators(path).split("@");
		if (parts.length == 0) {
			return "";
		}

		String pathInsideJar = parts[parts.length - 1];
		String pathWithoutExtension = StringUtils.removeLastPart(pathInsideJar, '.');
		return new JavaNames().getQualifiedClassName(pathWithoutExtension);
	}
}
