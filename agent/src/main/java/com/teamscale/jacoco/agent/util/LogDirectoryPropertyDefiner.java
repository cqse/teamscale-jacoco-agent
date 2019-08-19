package com.teamscale.jacoco.agent.util;

import ch.qos.logback.core.PropertyDefinerBase;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Defines a property that contains the default path to which log files should be written. */
public class LogDirectoryPropertyDefiner extends PropertyDefinerBase {
	@Override
	public String getPropertyValue() {
		Path agentDirectory = AgentUtils.getAgentDirectory();
		if (agentDirectory == null) {
			// we can't log the exception yet since logging is not yet initialized
			// fall back to the working directory
			return Paths.get(".").toAbsolutePath().toString();
		}
		return agentDirectory.resolve("logs").toAbsolutePath().toString();
	}
}
