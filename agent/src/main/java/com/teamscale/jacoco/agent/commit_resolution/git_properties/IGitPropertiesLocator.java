package com.teamscale.jacoco.agent.commit_resolution.git_properties;

import java.io.File;

/** Interface for the locator classes that search JAR files for git.properties files containing certain properties. */
public interface IGitPropertiesLocator {

	/** Searches the JAR file for the git.properties file containing certain properties. */
	void searchJarFileForGitPropertiesAsync(File jarFile);
}
