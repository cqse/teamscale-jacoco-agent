package com.teamscale.report.util

import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * This InputStream skips any initial bash script prepended to an executable spring-boot jar file,
 * positioning the stream at the start of the ZIP file header. This allows direct execution of
 * the jar without requiring "java -jar my.jar".
 */
class BashFileSkippingInputStream(input: InputStream) : FilterInputStream(BufferedInputStream(input)) {

	companion object {
		private val ZIP_HEADER = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
		private const val BUFFER_SIZE = 8192
	}

	init {
		skipToZipHeader()
	}

	/**
	 * Reads the stream until the ZIP file header (0x50 4B 03 04) is found, or EOF is reached.
	 * After calling this method, the read pointer is positioned at the first byte of the ZIP header.
	 */
	@Throws(IOException::class)
	private fun skipToZipHeader() {
		val buffer = ByteArray(BUFFER_SIZE)
		`in`.mark(BUFFER_SIZE)

		var bytesRead = `in`.read(buffer, 0, BUFFER_SIZE)
		while (bytesRead != -1) {
			val headerIndex = findZipHeader(buffer, bytesRead)
			if (headerIndex != -1) {
				// Reset and skip to the start of the ZIP header
				`in`.reset()
				`in`.skip(headerIndex.toLong())
				return
			}

			// Adjust position and re-mark to check the buffer boundary for header
			`in`.reset()
			`in`.skip((BUFFER_SIZE - ZIP_HEADER.size + 1).toLong())
			`in`.mark(BUFFER_SIZE)
			bytesRead = `in`.read(buffer, 0, BUFFER_SIZE)
		}

		throw IOException("ZIP header not found in the input stream.")
	}

	/**
	 * Searches the buffer for the ZIP header signature.
	 * @param buffer The buffer to search.
	 * @param length The number of valid bytes in the buffer.
	 * @return The index where the ZIP header starts, or -1 if not found.
	 */
	private fun findZipHeader(buffer: ByteArray, length: Int) =
		(0..length - ZIP_HEADER.size)
			.firstOrNull {
				buffer[it] == ZIP_HEADER[0]
						&& buffer[it + 1] == ZIP_HEADER[1]
						&& buffer[it + 2] == ZIP_HEADER[2]
						&& buffer[it + 3] == ZIP_HEADER[3]
			} ?: -1
}
