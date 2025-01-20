package com.teamscale.jacoco.agent.logging;

import java.nio.file.Path;

/** Defines a property that contains the path to which log files should be written. */
public class DebugLogDirectoryPropertyDefiner extends LogDirectoryPropertyDefiner {

	/** File path for debug logging. */
	/* package */ static Path filePath = null;

	@Override
	public String getPropertyValue() {
		if (filePath == null) {
			return super.getPropertyValue();
		}
		return filePath.resolve("logs").toAbsolutePath().toString();
	}
}
