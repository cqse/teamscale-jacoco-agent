package com.teamscale.report.util;

import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.StringUtils;
import org.jacoco.core.runtime.WildcardMatcher;
import org.jacoco.report.JavaNames;

/***
 * Tests given class file paths against call name patterns.
 * E.g. "/some/file/path/test.jar@my/package/Test.class" matches "my/package/*" or "my/package/Test"
 */
public class ClasspathWildcardIncludeFilter {

	/**
	 * Include patterns to apply during JaCoCo's traversal of class files. If null then everything is included.
	 */
	private WildcardMatcher locationIncludeFilters = null;

	/**
	 * Exclude patterns to apply during JaCoCo's traversal of class files. If null then nothing is excluded.
	 */
	private WildcardMatcher locationExcludeFilters = null;

	/**
	 * Constructor.
	 *
	 * @param locationIncludeFilters Colon separated list of wildcard include patterns for fully qualified class names
	 *                               or null for no includes. See {@link WildcardMatcher} for the pattern syntax.
	 * @param locationExcludeFilters Colon separated list of wildcard exclude patterns for fully qualified class names
	 *                               or null for no excludes.See {@link WildcardMatcher} for the pattern syntax.
	 */
	public ClasspathWildcardIncludeFilter(String locationIncludeFilters, String locationExcludeFilters) {
		if (locationIncludeFilters != null && !locationIncludeFilters.isEmpty()) {
			this.locationIncludeFilters = new WildcardMatcher(locationIncludeFilters);
		}
		if (locationExcludeFilters != null && !locationExcludeFilters.isEmpty()) {
			this.locationExcludeFilters = new WildcardMatcher(locationExcludeFilters);
		}
	}

	/**
	 * Tests if the given file path (e.g. "/some/file/path/test.jar@my/package/Test.class" or "org/mypackage/MyClass"
	 *
	 * @param path
	 * @return
	 */
	public boolean isIncluded(String path) {
		String className = getClassName(path);
		// first check includes
		if (locationIncludeFilters != null && !locationIncludeFilters.matches(className)) {
			return false;
		}
		// if they match, check excludes
		return locationExcludeFilters == null || !locationExcludeFilters.matches(className);
	}

	/**
	 * Returns the normalized class name of the given class file's path. I.e. turns something like
	 * "/opt/deploy/some.jar@com/teamscale/Class.class" into something like "com.teamscale.Class".
	 */
	/* package */
	static String getClassName(String path) {
		String[] parts = FileSystemUtils.normalizeSeparators(path).split("@");
		if (parts.length == 0) {
			return "";
		}

		String pathInsideJar = parts[parts.length - 1];
		if (path.toLowerCase().endsWith(".class")) {
			pathInsideJar = StringUtils.removeLastPart(pathInsideJar, '.');
		}
		return new JavaNames().getQualifiedClassName(pathInsideJar);
	}
}
