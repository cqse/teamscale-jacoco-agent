package com.teamscale.report.testwise.closure

import com.google.gson.Gson
import com.teamscale.report.testwise.closure.model.ClosureCoverage
import com.teamscale.report.testwise.model.TestwiseCoverage
import com.teamscale.report.testwise.model.builder.FileCoverageBuilder
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder
import org.conqat.lib.commons.filesystem.FileSystemUtils
import org.conqat.lib.commons.string.StringUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.util.*
import java.util.function.Predicate

/**
 * Creates [TestwiseCoverage] from Google closure coverage files. The given [ClosureCoverage] files must be
 * augmented with the [ClosureCoverage.uniformPath] field, which is not part of the Google closure coverage
 * specification.
 */
class ClosureTestwiseCoverageGenerator
/**
 * Create a new generator with a collection of report files.
 *
 * @param closureCoverageDirectories Root directory that contains the Google closure coverage reports.
 * @param locationIncludeFilter      Filter for js files
 */
    (
    /** Directories and zip files that contain closure coverage files.  */
    private val closureCoverageDirectories: Collection<File>,
    /** Include filter to apply to all js files contained in the original Closure coverage report.  */
    private val locationIncludeFilter: Predicate<String>
) {

    /**
     * Converts all JSON files in [.closureCoverageDirectories] to [TestCoverageBuilder]
     * and takes care of merging coverage distributed over multiple files.
     */
    fun readTestCoverage(): TestwiseCoverage {
        val testwiseCoverage = TestwiseCoverage()
        for (closureCoverageDirectory in closureCoverageDirectories) {
            if (closureCoverageDirectory.isFile) {
                testwiseCoverage.add(readTestCoverage(closureCoverageDirectory))
                continue
            }
            val coverageFiles = FileSystemUtils.listFilesRecursively(
                closureCoverageDirectory
            ) { file -> "json" == FileSystemUtils.getFileExtension(file) }
            for (coverageReportFile in coverageFiles) {
                testwiseCoverage.add(readTestCoverage(coverageReportFile))
            }
        }
        return testwiseCoverage
    }

    /**
     * Reads the given JSON file and converts its content to [TestCoverageBuilder].
     * If this fails for some reason the method returns null.
     */
    private fun readTestCoverage(file: File): TestCoverageBuilder? {
        try {
            val fileReader = FileReader(file)
            val coverage = Gson().fromJson(fileReader, ClosureCoverage::class.java)
            return convertToTestCoverage(coverage)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        return null
    }

    /** Converts the given [ClosureCoverage] to [TestCoverageBuilder].  */
    private fun convertToTestCoverage(coverage: ClosureCoverage): TestCoverageBuilder? {
        if (StringUtils.isEmpty(coverage.uniformPath)) {
            return null
        }
        val testCoverage = TestCoverageBuilder(coverage.uniformPath)
        val executedLines = coverage.fileNames.zip(coverage.executedLines)
        for ((fileName, coveredLines) in executedLines) {
            if (!locationIncludeFilter.test(fileName)) {
                continue
            }

            val coveredFile = File(fileName)
            val path = Optional.ofNullable(coveredFile.parent).orElse("")
            val fileCoverage = FileCoverageBuilder(path, coveredFile.name)
            for (i in coveredLines.indices) {
                if (coveredLines[i] == true) {
                    fileCoverage.addLine(i + 1)
                }
            }
            testCoverage.add(fileCoverage)
        }
        return testCoverage
    }
}