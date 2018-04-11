/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.agent;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses agent command line options.
 */
public class AgentOptions {

	/** The directories and/or zips that contain all class files being profiled. */
	public final List<File> classDirectoriesOrZips = new ArrayList<>();

	/**
	 * Ant-style include patterns to apply during JaCoCo's traversal of class files.
	 */
	public final List<String> locationIncludeFilters = new ArrayList<>();

	/**
	 * Ant-style exclude patterns to apply during JaCoCo's traversal of class files.
	 */
	public final List<String> locationExcludeFilters = new ArrayList<>();

	/** The directory to write the XML traces to. */
	public final Path outputDir = null;

	/** The interval in minutes for dumping XML data. */
	public final int dumpIntervalInMinutes = 0;

	/** Whether to ignore duplicate, non-identical class files. */
	public final boolean shouldIgnoreDuplicateClassFiles = false;

	/** Parses the given command-line options. */
	public AgentOptions(String options) {
	}

	public String createJacocoAgentOptions() {
		// TODO (FS)
		return null;
	}

}
