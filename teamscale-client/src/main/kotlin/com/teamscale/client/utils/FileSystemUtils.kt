package com.teamscale.client.utils

import java.io.*
import java.nio.charset.StandardCharsets

/**
 * File system utilities.
 */
object FileSystemUtils {
	/** Encoding for UTF-8.  */
	val UTF8_ENCODING: String = StandardCharsets.UTF_8.name()

	/** Unix file path separator  */
	private const val UNIX_SEPARATOR = '/'

	/**
	 * Checks if a directory exists. If not it creates the directory and all necessary parent directories.
	 *
	 * @throws IOException if directories couldn't be created.
	 */
	@Throws(IOException::class)
	fun ensureDirectoryExists(directory: File) {
		if (!directory.exists() && !directory.mkdirs()) {
			throw IOException("Couldn't create directory: $directory")
		}
	}

	/**
	 * Returns a list of all files and directories contained in the given directory and all subdirectories matching the
	 * filter provided. The given directory itself is not included in the result.
	 *
	 *
	 * The file filter may or may not exclude directories.
	 *
	 *
	 * This method knows nothing about (symbolic and hard) links, so care should be taken when traversing directories
	 * containing recursive links.
	 *
	 * @param directory the directory to start the search from. If this is null or the directory does not exist, an
	 * empty list is returned.
	 * @param filter    the filter used to determine whether the result should be included. If the filter is null, all
	 * files and directories are included.
	 * @return the list of files found (the order is determined by the file system).
	 */
	@JvmStatic
	fun listFilesRecursively(directory: File?, filter: FileFilter?): List<File> {
		if (directory == null || !directory.isDirectory) {
			return emptyList()
		}
		val result: MutableList<File> = ArrayList()
		listFilesRecursively(directory, result, filter)
		return result
	}

	/**
	 * Returns the extension of the file.
	 *
	 * @return File extension, i.e. "java" for "FileSystemUtils.java", or
	 * `null`, if the file has no extension (i.e. if a filename
	 * contains no '.'), returns the empty string if the '.' is the filename's last character.
	 */
	@JvmStatic
	fun getFileExtension(file: File): String? {
		val name = file.name
		val posLastDot = name.lastIndexOf('.')
		if (posLastDot < 0) {
			return null
		}
		return name.substring(posLastDot + 1)
	}

	/**
	 * Finds all files and directories contained in the given directory and all subdirectories matching the filter
	 * provided and put them into the result collection. The given directory itself is not included in the result.
	 *
	 *
	 * This method knows nothing about (symbolic and hard) links, so care should be taken when traversing directories
	 * containing recursive links.
	 *
	 * @param directory the directory to start the search from.
	 * @param result    the collection to add to all files found.
	 * @param filter    the filter used to determine whether the result should be included. If the filter is null, all
	 * files and directories are included.
	 */
	private fun listFilesRecursively(directory: File, result: MutableCollection<File>, filter: FileFilter?) {
		val files = directory.listFiles()
			?: // From the docs of `listFiles`:
// 		"If this abstract pathname does not denote a directory, then this method returns null."
// Based on this, it seems to be ok to just return here without throwing an exception.
			return

		for (file in files) {
			if (file.isDirectory) {
				listFilesRecursively(file, result, filter)
			}
			if (filter == null || filter.accept(file)) {
				result.add(file)
			}
		}
	}

	/**
	 * Replace platform dependent separator char with forward slashes to create system-independent paths.
	 */
	@JvmStatic
	fun normalizeSeparators(path: String): String {
		return path.replace(File.separatorChar, UNIX_SEPARATOR)
	}

	/**
	 * Copy an input stream to an output stream. This does *not* close the
	 * streams.
	 *
	 * @param input
	 * input stream
	 * @param output
	 * output stream
	 * @return number of bytes copied
	 * @throws IOException
	 * if an IO exception occurs.
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun copy(input: InputStream, output: OutputStream): Int {
		val buffer = ByteArray(1024)
		var size = 0
		var len: Int
		while ((input.read(buffer).also { len = it }) > 0) {
			output.write(buffer, 0, len)
			size += len
		}
		return size
	}

	/**
	 * Returns the name of the given file without extension. Example:
	 * '/home/joe/data.dat' returns 'data'.
	 */
	@JvmStatic
	fun getFilenameWithoutExtension(file: File): String? {
		return getFilenameWithoutExtension(file.name)
	}

	/**
	 * Returns the name of the given file without extension. Example: 'data.dat' returns 'data'.
	 */
	fun getFilenameWithoutExtension(fileName: String): String {
		return StringUtils.removeLastPart(fileName, '.')
	}
}