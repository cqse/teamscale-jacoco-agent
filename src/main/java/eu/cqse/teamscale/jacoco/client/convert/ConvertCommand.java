/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2017 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.client.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.conqat.lib.commons.assertion.CCSMAssert;
import org.conqat.lib.commons.collections.CollectionUtils;
import org.conqat.lib.commons.filesystem.FileSystemUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import eu.cqse.teamscale.jacoco.client.commandline.ICommand;

/**
 * Encapsulates all command line options for the convert command for parsing
 * with {@link JCommander}.
 */
@Parameters(commandNames = "convert", commandDescription = "Converts a binary .exec coverage file to XML.")
public class ConvertCommand implements ICommand {

	/** The directories and/or zips that contain all class files being profiled. */
	@Parameter(names = { "--classDir", "--jar", "-c" }, required = true, description = ""
			+ "The directories or zip/ear/jar/war/... files that contain the compiled Java classes being profiled."
			+ " Searches recursively, including inside zips.")
	/* package */ List<String> classDirectoriesOrZips = new ArrayList<>();

	/**
	 * Ant-style include patterns to apply during JaCoCo's traversal of class files.
	 */
	@Parameter(names = { "--filter", "-f" }, description = ""
			+ "Ant-style include patterns to apply to all found class file locations during JaCoCo's traversal of class files."
			+ " Note that zip contents are separated from zip files with @ and that you can filter only"
			+ " class files, not intermediate folders/zips. Use with great care as missing class files"
			+ " lead to broken coverage files! Turn on debug logging to see which locations are being filtered."
			+ " Defaults to no filtering. Excludes overrule includes.")
	/* package */ List<String> locationIncludeFilters = new ArrayList<>();

	/**
	 * Ant-style exclude patterns to apply during JaCoCo's traversal of class files.
	 */
	@Parameter(names = { "--exclude", "-e" }, description = ""
			+ "Ant-style exclude patterns to apply to all found class file locations during JaCoCo's traversal of class files."
			+ " Note that zip contents are separated from zip files with @ and that you can filter only"
			+ " class files, not intermediate folders/zips. Use with great care as missing class files"
			+ " lead to broken coverage files! Turn on debug logging to see which locations are being filtered."
			+ " Defaults to no filtering. Excludes overrule includes.")
	/* package */ List<String> locationExcludeFilters = new ArrayList<>();

	/** The directory to write the XML traces to. */
	@Parameter(names = { "--in", "-i" }, required = true, description = "" + "The binary .exec file to read.")
	/* package */ String inputFile = "";

	/** The directory to write the XML traces to. */
	@Parameter(names = { "--out", "-o" }, required = true, description = ""
			+ "The file to write the generated XML report to.")
	/* package */ String outputFile = "";

	/** Whether to ignore duplicate, non-identical class files. */
	@Parameter(names = { "--ignore-duplicates", "-d" }, required = false, description = ""
			+ "Whether to ignore duplicate, non-identical class files."
			+ " This is discouraged and may result in incorrect coverage files. Defaults to false.")
	/* package */ boolean shouldIgnoreDuplicateClassFiles = false;

	/** @see #classDirectoriesOrZips */
	public List<File> getClassDirectoriesOrZips() {
		return CollectionUtils.map(classDirectoriesOrZips, location -> new File(location));
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

	/** @see #inputFile */
	public File getInputFile() {
		return new File(inputFile);
	}

	/** @see #outputFile */
	public File getOutputFile() {
		return new File(outputFile);
	}

	/** @see #shouldIgnoreDuplicateClassFiles */
	public boolean shouldIgnoreDuplicateClassFiles() {
		return shouldIgnoreDuplicateClassFiles;
	}

	/** @see #shouldIgnoreDuplicateClassFiles */
	public void setShouldIgnoreDuplicateClassFiles(boolean shouldIgnoreDuplicateClassFiles) {
		this.shouldIgnoreDuplicateClassFiles = shouldIgnoreDuplicateClassFiles;
	}

	/** Makes sure the arguments are valid. */
	@Override
	public void validate() throws IOException {
		for (File path : getClassDirectoriesOrZips()) {
			CCSMAssert.isTrue(path.exists(), "Path '" + path + "' does not exist");
			CCSMAssert.isTrue(path.canRead(), "Path '" + path + "' is not readable");
		}

		File outputDir = getInputFile().getParentFile();
		FileSystemUtils.ensureDirectoryExists(outputDir);
		CCSMAssert.isTrue(outputDir.canWrite(), "Path '" + outputDir + "' is not writable");
	}

	/** {@inheritDoc} */
	@Override
	public void run() throws Exception {
		new Converter(this).run();
	}

}