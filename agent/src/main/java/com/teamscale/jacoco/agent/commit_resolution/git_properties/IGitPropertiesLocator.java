package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import java.io.File;

/** Interface for the locator classes that search files (e.g., a JAR) for git.properties files containing certain properties. */
public interface IGitPropertiesLocator {

	/**
	 * Searches the file for the git.properties file containing certain properties. The boolean flag indicates whether the
	 * searched file is a JAR file or a plain directory.
	 */
	void searchFileForGitPropertiesAsync(File file, boolean isJarFile);
}
