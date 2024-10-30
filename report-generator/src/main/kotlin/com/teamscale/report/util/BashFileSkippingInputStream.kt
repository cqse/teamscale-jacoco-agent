package com.teamscale.report.util

import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Handles executable spring-boot jar files that prepend a bash file to the beginning of the ZIP file to make it
 * directly executable without "java -jar my.jar". We just skip the bash file until we find the zip file header.
 */
class BashFileSkippingInputStream(`in`: InputStream) : FilterInputStream(BufferedInputStream(`in`)) {
	/**
	 * Wraps the given input stream in a BufferedInputStream and consumes all bytes until a zip file header is found.
	 */
	init {
		consumeUntilZipHeader()
	}

	/**
	 * Reads the stream until the zip file header "50 4B 03 04" is found or EOF is reached. After calling the method the
	 * read pointer points to the first byte of the zip file header.
	 */
	@Throws(IOException::class)
	private fun consumeUntilZipHeader() {
		val buffer = ByteArray(8192)
		`in`.mark(buffer.size)
		var count = `in`.read(buffer, 0, buffer.size)
		while (count > 0) {
			for (i in 0 until count - 3) {
				if (buffer[i].toInt() == 0x50 && buffer[i + 1].toInt() == 0x4B && buffer[i + 2].toInt() == 0x03 && buffer[i + 3].toInt() == 0x04) {
					`in`.reset()
					`in`.skip(i.toLong())
					return
				}
			}

			// Reset mark to 3 bytes before the end of the previously read buffer end to
			// also detect a zip header when it spans over two buffers
			`in`.reset()
			`in`.skip((buffer.size - 3).toLong())
			`in`.mark(buffer.size)
			count = `in`.read(buffer, 0, buffer.size)
		}
	}
}
