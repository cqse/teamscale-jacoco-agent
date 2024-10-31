package com.teamscale.report

import com.fasterxml.jackson.core.JsonProcessingException
import com.teamscale.client.FileSystemUtils
import com.teamscale.client.JsonUtils
import com.teamscale.client.TestDetails
import com.teamscale.report.testwise.ETestArtifactFormat
import com.teamscale.report.testwise.model.TestExecution
import com.teamscale.report.testwise.model.TestwiseCoverageReport
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.*

/** Utilities for generating reports.  */
object ReportUtils {
	/** Converts to given test list to a json report and writes it to the given file.  */
	@Throws(IOException::class)
	@JvmStatic
	fun writeTestListReport(reportFile: File, report: List<TestDetails>) {
		writeReportToFile(reportFile, report)
	}

	/** Converts to given test execution report to a json report and writes it to the given file.  */
	@Throws(IOException::class)
	@JvmStatic
	fun writeTestExecutionReport(reportFile: File, report: List<TestExecution>) {
		writeReportToFile(reportFile, report)
	}

	/** Converts to given testwise coverage report to a json report and writes it to the given file.  */
	@Throws(IOException::class)
	fun writeTestwiseCoverageReport(reportFile: File, report: TestwiseCoverageReport) {
		writeReportToFile(reportFile, report)
	}

	/** Converts to given report to a json string. For testing only.  */
	@JvmStatic
	@Throws(JsonProcessingException::class)
	fun getTestwiseCoverageReportAsString(
		report: TestwiseCoverageReport?
	): String {
		return JsonUtils.serialize(report)
	}

	/** Writes the report object to the given file as json.  */
	@Throws(IOException::class)
	private fun <T> writeReportToFile(reportFile: File, report: T) {
		val directory: File = reportFile.getParentFile()
		if (!directory.isDirectory() && !directory.mkdirs()) {
			throw IOException("Failed to create directory " + directory.absolutePath)
		}
		JsonUtils.serializeToFile(reportFile, report)
	}

	/** Recursively lists all files in the given directory that match the specified extension.  */
	@Throws(IOException::class)
	@JvmStatic
	fun <T> readObjects(
		format: ETestArtifactFormat, clazz: Class<Array<T>>?,
		directoriesOrFiles: List<File>
	): List<T> {
		val files: List<File> = listFiles(format, directoriesOrFiles)
		val result: ArrayList<T> = ArrayList()
		for (file: File? in files) {
			val t: Array<T>? = JsonUtils.deserializeFile(file, clazz)
			if (t != null) {
				result.addAll(listOf(*t))
			}
		}
		return result
	}

	/** Recursively lists all files of the given artifact type.  */
	@JvmStatic
	fun listFiles(format: ETestArtifactFormat, directoriesOrFiles: List<File>): List<File> {
		val filesWithSpecifiedArtifactType: MutableList<File> = ArrayList()
		for (directoryOrFile: File in directoriesOrFiles) {
			if (directoryOrFile.isDirectory()) {
				filesWithSpecifiedArtifactType.addAll(
					FileSystemUtils.listFilesRecursively(directoryOrFile) {
						it.isOfArtifactFormat(format)
					}
				)
			} else if (directoryOrFile.isOfArtifactFormat(format)) {
				filesWithSpecifiedArtifactType.add(directoryOrFile)
			}
		}
		return filesWithSpecifiedArtifactType
	}

	private fun File.isOfArtifactFormat(format: ETestArtifactFormat) =
		isFile() &&
				getName().startsWith(format.filePrefix) &&
				FileSystemUtils.getFileExtension(this).equals(format.extension, ignoreCase = true)
}
