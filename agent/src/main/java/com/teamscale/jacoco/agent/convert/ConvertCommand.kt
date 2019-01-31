/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2017 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.jacoco.agent.convert

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.teamscale.jacoco.agent.commandline.ICommand
import com.teamscale.jacoco.agent.commandline.Validator
import org.conqat.lib.commons.assertion.CCSMAssert
import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.conqat.lib.commons.string.StringUtils
import java.io.File
import java.util.*

/**
 * Encapsulates all command line options for the convert command for parsing
 * with [JCommander].
 */
@Parameters(commandNames = arrayOf("convert"), commandDescription = "Converts a binary .exec coverage file to XML.")
class ConvertCommand : ICommand {

    /** The directories and/or zips that contain all class files being profiled.  */
    @Parameter(
        names = arrayOf("--classDir", "--jar", "-c"), required = true, description = ""
                + "The directories or zip/ear/jar/war/... files that contain the compiled Java classes being profiled."
                + " Searches recursively, including inside zips."
    )
    internal var classDirectoriesOrZips: List<String> = ArrayList()/* package */

    /**
     * Ant-style include patterns to apply during JaCoCo's traversal of class files.
     */
    /** @see .locationIncludeFilters
     */
    /** @see .locationIncludeFilters
     */
    @Parameter(
        names = arrayOf("--filter", "-f"), description = ""
                + "Ant-style include patterns to apply to all found class file locations during JaCoCo's traversal of class files."
                + " Note that zip contents are separated from zip files with @ and that you can filter only"
                + " class files, not intermediate folders/zips. Use with great care as missing class files"
                + " lead to broken coverage files! Turn on debug logging to see which locations are being filtered."
                + " Defaults to no filtering. Excludes overrule includes."
    )
            /* package */ var locationIncludeFilters: List<String> = ArrayList()

    /**
     * Ant-style exclude patterns to apply during JaCoCo's traversal of class files.
     */
    /** @see .locationExcludeFilters
     */
    /** @see .locationExcludeFilters
     */
    @Parameter(
        names = arrayOf("--exclude", "-e"), description = ""
                + "Ant-style exclude patterns to apply to all found class file locations during JaCoCo's traversal of class files."
                + " Note that zip contents are separated from zip files with @ and that you can filter only"
                + " class files, not intermediate folders/zips. Use with great care as missing class files"
                + " lead to broken coverage files! Turn on debug logging to see which locations are being filtered."
                + " Defaults to no filtering. Excludes overrule includes."
    )
            /* package */ var locationExcludeFilters: List<String> = ArrayList()

    /** The directory to write the XML traces to.  */
    @Parameter(
        names = arrayOf("--in", "-i"),
        required = true,
        description = "" + "The binary .exec file(s), test details and " +
                "test executions to read"
    )
    internal var inputFiles: List<String> = ArrayList()/* package */

    /** The directory to write the XML traces to.  */
    @Parameter(
        names = arrayOf("--out", "-o"),
        required = true,
        description = "" + "The file to write the generated XML report to."
    )
    internal var outputFile = ""/* package */

    /** Whether to ignore duplicate, non-identical class files.  */
    @Parameter(
        names = arrayOf("--ignore-duplicates", "-d"), required = false, arity = 1, description = ""
                + "Whether to ignore duplicate, non-identical class files."
                + " This is discouraged and may result in incorrect coverage files. Defaults to false."
    )
    internal var shouldIgnoreDuplicateClassFiles = false/* package */

    /** Whether to ignore duplicate, non-identical class files.  */
    @Parameter(
        names = arrayOf("--testwise-coverage", "-t"),
        required = false,
        arity = 0,
        description = "Whether testwise " + "coverage or jacoco coverage should be generated."
    )
    internal var shouldGenerateTestwiseCoverage = false/* package */

    /** @see .classDirectoriesOrZips
     */
    fun getClassDirectoriesOrZips(): List<File> {
        return classDirectoriesOrZips.map { File(it) }
    }

    /** @see .classDirectoriesOrZips
     */
    fun setClassDirectoriesOrZips(classDirectoriesOrZips: List<String>) {
        this.classDirectoriesOrZips = classDirectoriesOrZips
    }

    /** @see .inputFiles
     */
    fun getInputFiles(): List<File> {
        return inputFiles.map { File(it) }
    }

    /** @see .outputFile
     */
    fun getOutputFile(): File {
        return File(outputFile)
    }

    /** @see .shouldIgnoreDuplicateClassFiles
     */
    fun shouldIgnoreDuplicateClassFiles(): Boolean {
        return shouldIgnoreDuplicateClassFiles
    }

    /** @see .shouldIgnoreDuplicateClassFiles
     */
    fun setShouldIgnoreDuplicateClassFiles(shouldIgnoreDuplicateClassFiles: Boolean) {
        this.shouldIgnoreDuplicateClassFiles = shouldIgnoreDuplicateClassFiles
    }

    /** Makes sure the arguments are valid.  */
    override fun validate(): Validator {
        val validator = Validator()

        validator.isFalse(
            getClassDirectoriesOrZips().isEmpty(),
            "You must specify at least one directory or zip that contains class files"
        )
        for (path in getClassDirectoriesOrZips()) {
            validator.isTrue(path.exists(), "Path '$path' does not exist")
            validator.isTrue(path.canRead(), "Path '$path' is not readable")
        }

        for (inputFile in getInputFiles()) {
            validator.isTrue(
                inputFile.exists() && inputFile.canRead(),
                "Cannot read the input file $inputFile"
            )
        }

        validator.ensure({
            CCSMAssert.isFalse(StringUtils.isEmpty(outputFile), "You must specify an output file")
            val outputDir = getOutputFile().absoluteFile.parentFile
            FileSystemUtils.ensureDirectoryExists(outputDir)
            CCSMAssert.isTrue(outputDir.canWrite(), "Path '$outputDir' is not writable")
        })

        return validator
    }

    /** {@inheritDoc}  */
    @Throws(Exception::class)
    override fun run() {
        val converter = Converter(this)
        if (this.shouldGenerateTestwiseCoverage) {
            converter.runTestwiseCoverageReportGeneration()
        } else {
            converter.runJaCoCoReportGeneration()
        }
    }
}