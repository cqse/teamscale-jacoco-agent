package com.teamscale.report.util

import java.io.File
import java.io.IOException

object FileSystemUtils {

    /** Unix file path separator  */
    private const val UNIX_SEPARATOR = '/'

    /**
     * Replace platform dependent separator char with forward slashes to create
     * system-independent paths.
     */
    fun normalizeSeparators(path: String): String {
        return path.replace(File.separatorChar, UNIX_SEPARATOR)
    }

    /**
     * Checks if a directory exists. If not it creates the directory and all
     * necessary parent directories.
     *
     * @throws IOException
     * if directories couldn't be created.
     */
    @Throws(IOException::class)
    fun ensureDirectoryExists(directory: File) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Couldn't create directory: $directory")
        }
    }
}