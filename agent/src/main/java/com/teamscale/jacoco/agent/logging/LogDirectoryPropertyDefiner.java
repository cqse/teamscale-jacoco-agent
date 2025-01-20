package com.teamscale.jacoco.agent.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import com.teamscale.jacoco.agent.util.AgentUtils;

import java.nio.file.Path;

/** Defines a property that contains the default path to which log files should be written. */
public class LogDirectoryPropertyDefiner extends PropertyDefinerBase {
	@Override
	public String getPropertyValue() {
		Path tempDirectory = AgentUtils.getMainTempDirectory();
		return tempDirectory.resolve("logs").toAbsolutePath().toString();
	}
}
