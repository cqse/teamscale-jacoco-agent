package com.teamscale.report

import com.teamscale.client.utils.FileSystemUtils
import com.teamscale.client.utils.JsonUtils.deserializeAsArray
import com.teamscale.client.utils.JsonUtils.serializeWriteToFile
import com.teamscale.report.testwise.ETestArtifactFormat
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


/** Utilities for generating reports. */
object ReportUtils {

	/** Writes the report object to the given file as json. */
	@Throws(IOException::class)
	@JvmStatic
	fun <T> writeReportToFile(reportFile: File, report: T) {
		val directory = reportFile.parentFile
		if (!directory.isDirectory && !directory.mkdirs()) {
			throw IOException("Failed to create directory ${directory.absolutePath}")
		}
		report.serializeWriteToFile(reportFile)
	}

	/** Recursively lists all files in the given directory that match the specified extension. */
	@JvmStatic inline fun <reified T : Any> List<File>.readObjects(format: ETestArtifactFormat) =
		filterByFormat(format).flatMap { it.deserializeAsArray<T>() }

	/** Recursively lists all files of the given artifact type. */
	@JvmStatic fun List<File>.filterByFormat(format: ETestArtifactFormat) =
		flatMap { file ->
			if (file.isDirectory) {
				FileSystemUtils.listFilesRecursively(file) {
					it.isOfArtifactFormat(format)
				}
			} else if (file.isOfArtifactFormat(format)) {
				listOf(file)
			} else {
				emptyList()
			}
		}

	/** Recursively lists all files in the given directory that match the specified extension.  */
	// ToDo: Remove when System tests are in Kotlin
	@Throws(IOException::class)
	@JvmStatic fun <T : Any> readObjects(
		format: ETestArtifactFormat,
		clazz: Class<T>,
		directoriesOrFiles: List<File>
	): List<T> {
		val files = directoriesOrFiles.filterByFormat(format)
		val result = ArrayList<T>()
		for (file in files) {
			val t: ArrayList<T> = file.deserializeAsArray(clazz)
			result.addAll(t)
		}
		return result
	}

	private fun File.isOfArtifactFormat(format: ETestArtifactFormat) =
		isFile &&
			name.startsWith(format.filePrefix) &&
			FileSystemUtils.getFileExtension(this).equals(format.extension, ignoreCase = true)
}