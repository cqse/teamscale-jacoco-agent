package com.teamscale.jacoco.agent.git_properties;

/**
 * Thrown in case a git.properties file is found but it is malformed.
 */
public class InvalidGitPropertiesException extends Exception {
	/*package*/ InvalidGitPropertiesException(String s, Throwable throwable) {
		super(s, throwable);
	}

	public InvalidGitPropertiesException(String s) {
		super(s);
	}
}
