package com.teamscale.report.jacoco

import com.teamscale.client.FileSystemUtils
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.*
import java.nio.file.Files
import java.util.*

/**
 * Represents a coverage file on disk. The main purpose is to avoid reading the
 * entire file into memory as this dramatically increases the memory footprint
 * of the JVM which might run out of memory because of this.
 *
 * The object internally holds a counter of how many references to the file are
 * currently held. This allows to share the same file for multiple uploads and
 * deleting it once all uploads have succeeded. Use [.acquireReference]
 * to make the object aware that it was passed to another uploader and
 * [.delete] to signal that you no longer intend to access the file.
 */
class CoverageFile(private val coverageFile: File) {
	private var referenceCounter = 0

	/**
	 * Marks the file as being used by an additional uploader. This ensures that the
	 * file is not deleted until all users have signed via [.delete] that
	 * they no longer intend to access the file.
	 */
	fun acquireReference(): CoverageFile {
		referenceCounter++
		return this
	}

	/**
	 * Copies the coverage File in blocks from the disk to the output stream to
	 * avoid having to read the entire file into memory.
	 */
	@Throws(IOException::class)
	fun copy(outputStream: OutputStream?) {
		val inputStream = FileInputStream(coverageFile)
		FileSystemUtils.copy(inputStream, outputStream)
		inputStream.close()
	}

	val nameWithoutExtension: String
		/**
		 * Get the filename of the coverage file on disk without its extension
		 */
		get() = FileSystemUtils.getFilenameWithoutExtension(coverageFile)

	val name: String
		/** Get the filename of the coverage file.  */
		get() = coverageFile.name

	/**
	 * Delete the coverage file from disk
	 */
	@Throws(IOException::class)
	fun delete() {
		referenceCounter--
		if (referenceCounter <= 0) {
			Files.delete(coverageFile.toPath())
		}
	}

	/**
	 * Create a [okhttp3.MultipartBody] form body with the contents of the
	 * coverage file.
	 */
	fun createFormRequestBody(): RequestBody {
		return RequestBody.create(MultipartBody.FORM, File(coverageFile.absolutePath))
	}

	@get:Throws(IOException::class)
	val outputStream: OutputStream
		/**
		 * Get the [java.io.OutputStream] in order to write to the coverage file.
		 *
		 * @throws IOException
		 * If the file did not exist yet and could not be created
		 */
		get() {
			try {
				return FileOutputStream(coverageFile)
			} catch (e: IOException) {
				throw IOException(
					("Could not create temporary coverage file" + this + ". "
							+ "This is used to cache the coverage file on disk before uploading it to its final destination. "
							+ "This coverage is lost. Please fix the underlying issue to avoid losing coverage."), e
				)
			}
		}

	/**
	 * {@inheritDoc}
	 */
	override fun equals(o: Any?): Boolean {
		if (this === o) {
			return true
		}
		if (o == null || javaClass != o.javaClass) {
			return false
		}
		val that = o as CoverageFile
		return coverageFile == that.coverageFile
	}

	/**
	 * {@inheritDoc}
	 */
	override fun hashCode(): Int {
		return Objects.hash(coverageFile)
	}

	/**
	 * {@inheritDoc}
	 */
	override fun toString(): String {
		return coverageFile.absolutePath
	}
}