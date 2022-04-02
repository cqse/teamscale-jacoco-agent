package com.teamscale.tia.maven;

import org.apache.maven.project.MavenProject;
import shadow.org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;

public class ArgLine {

	/**
	 * Name of the property used in the maven-osgi-test-plugin.
	 */
	private static final String TYCHO_ARG_LINE = "tycho.testArgLine";

	/**
	 * Name of the property used in the maven-surefire-plugin.
	 */
	private static final String SUREFIRE_ARG_LINE = "argLine";

	private final String additionalAgentOptions;
	private final String agentLogLevel;
	private final Path agentJarFile;
	private final Path agentConfigFile;
	private final Path logFilePath;

	public ArgLine(String additionalAgentOptions, String agentLogLevel, Path agentJarFile,
				   Path agentConfigFile, Path logFilePath) {
		this.additionalAgentOptions = additionalAgentOptions;
		this.agentLogLevel = agentLogLevel;
		this.agentJarFile = agentJarFile;
		this.agentConfigFile = agentConfigFile;
		this.logFilePath = logFilePath;
	}

	public String prependTo(String oldArgLine) {
		String suffix = removePreviousTiaAgent(oldArgLine);
		if (StringUtils.isNotBlank(suffix)) {
			suffix = " " + suffix;
		}
		return createJvmOptions() + suffix;
	}

	private String createJvmOptions() {
		String jvmOptions = "'" + createUnquotedJavaagentArgument() + "'" +
				" '-DTEAMSCALE_AGENT_LOG_FILE=" + logFilePath + "'" +
				" -DTEAMSCALE_AGENT_LOG_LEVEL=" + agentLogLevel;
		return "-Dteamscale.markstart " + jvmOptions + " -Dteamscale.markend";
	}

	private String createUnquotedJavaagentArgument() {
		String javaagentArgument = "-javaagent:" + agentJarFile.toAbsolutePath()
				+ "=config-file=" + agentConfigFile.toAbsolutePath();
		if (StringUtils.isNotBlank(additionalAgentOptions)) {
			javaagentArgument += "," + additionalAgentOptions;
		}
		return javaagentArgument;
	}

	public static String getEffectivePropertyName(String userDefinedPropertyName, MavenProject mavenProject) {
		if (StringUtils.isNotBlank(userDefinedPropertyName)) {
			return userDefinedPropertyName;
		}
		if ("eclipse-test-plugin".equals(mavenProject.getPackaging())) {
			return TYCHO_ARG_LINE;
		}
		return SUREFIRE_ARG_LINE;
	}

	/**
	 * Removes any previous invocation of our agent from the given argLine.
	 * This is necessary in case we want to instrument unit and integration tests but with different arguments.
	 */
	private static String removePreviousTiaAgent(String argLine) {
		if (argLine == null) {
			return null;
		}
		return argLine.replaceAll("-Dteamscale.markstart.*teamscale.markend", "");
	}

}
