package com.teamscale.report

import com.google.gson.GsonBuilder
import com.teamscale.report.testwise.ETestArtifactFormat
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*

/** Utilities for generating reports.  */
object ReportUtils {

    private val GSON = GsonBuilder().setPrettyPrinting().create()

    /** Converts to given testwise coverage report to a json report and writes it to the given file.  */
    @Throws(IOException::class)
    fun <T> writeReportToFile(reportFile: File, report: T) {
        val directory = reportFile.parentFile
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Failed to create directory " + directory.absolutePath)
        }
        FileWriter(reportFile).use { writer -> GSON.toJson(report, writer) }
    }

    /** Converts to given report to a json string.  */
    fun <T> getReportAsString(report: T): String {
        return GSON.toJson(report)
    }

    /** Recursively lists all files in the given directory that match the specified extension.  */
    @Throws(IOException::class)
    fun <T> readObjects(format: ETestArtifactFormat, clazz: Class<Array<T>>, vararg directoriesOrFiles: File): List<T> {
        return readObjects(format, clazz, Arrays.asList(*directoriesOrFiles))
    }

    /** Recursively lists all files in the given directory that match the specified extension.  */
    @Throws(IOException::class)
    fun <T> readObjects(format: ETestArtifactFormat, clazz: Class<Array<T>>, directoriesOrFiles: List<File>): List<T> {
        val files = listFiles(format, directoriesOrFiles)
        val result = ArrayList<T>()
        for (file in files) {
            FileReader(file).use { reader -> result.addAll(Arrays.asList(*GSON.fromJson(reader, clazz))) }
        }
        return result
    }

    /** Recursively lists all files of the given artifact type.  */
    fun listFiles(format: ETestArtifactFormat, vararg directoriesOrFiles: File): List<File> {
        return listFiles(format, Arrays.asList(*directoriesOrFiles))
    }

    /** Recursively lists all files of the given artifact type.  */
    fun listFiles(format: ETestArtifactFormat, directoriesOrFiles: List<File>): List<File> {
        return directoriesOrFiles.flatMap { directory ->
            directory.walk().filter { pathname ->
                pathname.isFile && pathname.name.startsWith(format.filePrefix) && pathname.extension.equals(
                    format.extension,
                    ignoreCase = true
                )
            }.toList()
        }
    }
}
