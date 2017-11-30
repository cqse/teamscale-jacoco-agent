/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2017 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.watch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.filesystem.FileSystemUtils;
import org.conqat.lib.commons.string.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import eu.cqse.teamscale.jacoco.client.commandline.ICommand;

/**
 * Encapsulates all command line options for the watch command for parsing with
 * {@link JCommander}.
 */
@Parameters(commandNames = "watch", commandDescription = "Watches a running JaCoCo instance via a TCP port and"
		+ " regularly dumps coverage to XML files.")
public class WatchCommand implements ICommand {

	/** The directories and/or zips that contain all class files being profiled. */
	@Parameter(names = { "--classDir", "--jar", "-c" }, required = true, description = ""
			+ "The directories or zip/ear/jar/war/... files that contain the compiled Java classes being profiled."
			+ " Searches recursively, including inside zips.")
	private List<String> classDirectoriesOrZips = new ArrayList<>();

	/**
	 * Ant-style include patterns to apply during JaCoCo's traversal of class files.
	 */
	@Parameter(names = { "--filter", "-f" }, description = ""
			+ "Ant-style include patterns to apply to all found class file locations during JaCoCo's traversal of class files."
			+ " Note that zip contents are separated from zip files with @ and that you can filter only"
			+ " class files, not intermediate folders/zips. Use with great care as missing class files"
			+ " lead to broken coverage files! Turn on debug logging to see which locations are being filtered."
			+ " Defaults to no filtering. Excludes overrule includes.")
	private List<String> locationIncludeFilters = new ArrayList<>();

	/**
	 * Ant-style exclude patterns to apply during JaCoCo's traversal of class files.
	 */
	@Parameter(names = { "--exclude", "-e" }, description = ""
			+ "Ant-style exclude patterns to apply to all found class file locations during JaCoCo's traversal of class files."
			+ " Note that zip contents are separated from zip files with @ and that you can filter only"
			+ " class files, not intermediate folders/zips. Use with great care as missing class files"
			+ " lead to broken coverage files! Turn on debug logging to see which locations are being filtered."
			+ " Defaults to no filtering. Excludes overrule includes.")
	private List<String> locationExcludeFilters = new ArrayList<>();

	/** The JaCoCo port. */
	@Parameter(names = { "--port", "-p" }, required = true, description = ""
			+ "The port under which JaCoCo is listening for connections.")
	private int port = 0;

	/** The directory to write the XML traces to. */
	@Parameter(names = { "--out", "-o" }, required = true, description = ""
			+ "The directory to write the generated XML reports to.")
	private String outputDir = "";

	/** The interval in minutes for dumping XML data. */
	@Parameter(names = { "--interval", "-i" }, required = true, description = ""
			+ "Interval in minutes after which the current coverage is retrived and stored in a new XML file.")
	private int dumpIntervalInMinutes = 0;

	/** The interval in seconds for a reconnect to the application. */
	@Parameter(names = { "--reconnect", "-r" }, required = false, description = ""
			+ "Interval in seconds after which to try and reconnect after losing connection to JaCoCo.")
	private int reconnectIntervalInSeconds = 5 * 60;

	/** Whether to ignore duplicate, non-identical class files. */
	@Parameter(names = { "--ignore-duplicates", "-d" }, required = false, description = ""
			+ "Whether to ignore duplicate, non-identical class files."
			+ " This is discouraged and may result in incorrect coverage files. Defaults to false.")
	private boolean shouldIgnoreDuplicateClassFiles = false;

	/** @see #classDirectoriesOrZips */
	public List<String> getClassDirectoriesOrZips() {
		return classDirectoriesOrZips;
	}

	/** @see #classDirectoriesOrZips */
	public void setClassDirectoriesOrZips(List<String> classDirectoriesOrZips) {
		this.classDirectoriesOrZips = classDirectoriesOrZips;
	}

	/** @see #locationIncludeFilters */
	public List<String> getLocationIncludeFilters() {
		return locationIncludeFilters;
	}

	/** @see #locationIncludeFilters */
	public void setLocationIncludeFilters(List<String> locationIncludeFilters) {
		this.locationIncludeFilters = locationIncludeFilters;
	}

	/** @see #locationExcludeFilters */
	public List<String> getLocationExcludeFilters() {
		return locationExcludeFilters;
	}

	/** @see #locationExcludeFilters */
	public void setLocationExcludeFilters(List<String> locationExcludeFilters) {
		this.locationExcludeFilters = locationExcludeFilters;
	}

	/** @see #port */
	public int getPort() {
		return port;
	}

	/** @see #port */
	public void setPort(int port) {
		this.port = port;
	}

	/** @see #outputDir */
	public String getOutputDir() {
		return outputDir;
	}

	/** @see #outputDir */
	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	/** @see #dumpIntervalInMinutes */
	public int getDumpIntervalInMinutes() {
		return dumpIntervalInMinutes;
	}

	/** @see #dumpIntervalInMinutes */
	public void setDumpIntervalInMinutes(int dumpIntervalInMinutes) {
		this.dumpIntervalInMinutes = dumpIntervalInMinutes;
	}

	/** @see #reconnectIntervalInSeconds */
	public int getReconnectIntervalInSeconds() {
		return reconnectIntervalInSeconds;
	}

	/** @see #reconnectIntervalInSeconds */
	public void setReconnectIntervalInSeconds(int reconnectIntervalInSeconds) {
		this.reconnectIntervalInSeconds = reconnectIntervalInSeconds;
	}

	/** @see #shouldIgnoreDuplicateClassFiles */
	public boolean isShouldIgnoreDuplicateClassFiles() {
		return shouldIgnoreDuplicateClassFiles;
	}

	/** @see #shouldIgnoreDuplicateClassFiles */
	public void setShouldIgnoreDuplicateClassFiles(boolean shouldIgnoreDuplicateClassFiles) {
		this.shouldIgnoreDuplicateClassFiles = shouldIgnoreDuplicateClassFiles;
	}

	/** {@inheritDoc} */
	@Override
	public void validate() throws IOException {
		for (String path : getClassDirectoriesOrZips()) {
			CCSMAssert.isTrue(new File(path).exists(), "Path '" + path + "' does not exist");
			CCSMAssert.isTrue(new File(path).canRead(), "Path '" + path + "' is not readable");
		}

		CCSMAssert.isFalse(StringUtils.isEmpty(getOutputDir()), "You must specify an output directory");
		FileSystemUtils.ensureDirectoryExists(new File(getOutputDir()));
		CCSMAssert.isTrue(new File(getOutputDir()).canWrite(), "Path '" + getOutputDir() + "' is not writable");
	}

	/** {@inheritDoc} */
	@Override
	public void run() {
		new Watcher(this).loop();
	}

}