/*******************************************************************************
 * Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Marc R. Hoffmann - initial API and implementation
 *
 */
package com.teamscale.report.jacoco

import org.jacoco.core.JaCoCo
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.internal.ContentTypeDetector
import org.jacoco.core.internal.InputStreams
import org.jacoco.core.internal.Pack200Streams
import org.jacoco.core.internal.analysis.ClassAnalyzer
import org.jacoco.core.internal.analysis.ClassCoverageImpl
import org.jacoco.core.internal.analysis.StringPool
import org.jacoco.core.internal.data.CRC64
import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.jacoco.core.internal.instr.InstrSupport
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * This is a copy of the [Analyzer] class from JaCoCo.
 * The only changes are that the following methods are protected instead of private:
 * - [.analyzeClass]
 * - [.analyzerError]
 *
 *
 * When performing an update of JaCoCo we need to check that this file is still up-to-date.
 *
 *
 * An [Analyzer] instance processes a set of Java class files and
 * calculates coverage data for them. For each class file the result is reported
 * to a given [ICoverageVisitor] instance. In addition the
 * [Analyzer] requires a [ExecutionDataStore] instance that holds
 * the execution data for the classes to analyze. The [Analyzer] offers
 * several methods to analyze classes from a variety of sources.
 */
open class OpenAnalyzer(
	private val executionData: ExecutionDataStore?,
	private val coverageVisitor: ICoverageVisitor?
) {
	private val stringPool = StringPool()

	/**
	 * Creates an ASM class visitor for analysis.
	 *
	 * @param classId
	 * id of the class calculated with [CRC64]
	 * @param className
	 * VM name of the class
	 * @return ASM visitor to write class definition to
	 */
	private fun createAnalyzingVisitor(
		classId: Long,
		className: String
	): ClassVisitor {
		val data = executionData!![classId]
		val probes: BooleanArray?
		val noMatch: Boolean
		if (data == null) {
			probes = null
			noMatch = executionData.contains(className)
		} else {
			probes = data.probes
			noMatch = false
		}
		val coverage = ClassCoverageImpl(
			className,
			classId, noMatch
		)
		val analyzer: ClassAnalyzer = object : ClassAnalyzer(
			coverage, probes,
			stringPool
		) {
			override fun visitEnd() {
				super.visitEnd()
				coverageVisitor!!.visitCoverage(coverage)
			}
		}
		return ClassProbesAdapter(analyzer, false)
	}

	/** Analyzes the given class in binary form.  */
	protected open fun analyzeClass(source: ByteArray) {
		val classId = CRC64.classId(source)
		val reader = InstrSupport.classReaderFor(source)
		if ((reader.access and Opcodes.ACC_MODULE) != 0) {
			return
		}
		if ((reader.access and Opcodes.ACC_SYNTHETIC) != 0) {
			return
		}
		val visitor = createAnalyzingVisitor(
			classId,
			reader.className
		)
		reader.accept(visitor, 0)
	}

	/**
	 * Analyzes the class definition from a given in-memory buffer.
	 *
	 * @param buffer
	 * class definitions
	 * @param location
	 * a location description used for exception messages
	 * @throws IOException
	 * if the class can't be analyzed
	 */
	@Throws(IOException::class)
	open fun analyzeClass(buffer: ByteArray, location: String) {
		try {
			analyzeClass(buffer)
		} catch (cause: RuntimeException) {
			throw analyzerError(location, cause)
		}
	}

	/**
	 * Analyzes the class definition from a given input stream. The provided
	 * [InputStream] is not closed by this method.
	 *
	 * @param input
	 * stream to read class definition from
	 * @param location
	 * a location description used for exception messages
	 * @throws IOException
	 * if the stream can't be read or the class can't be analyzed
	 */
	@Throws(IOException::class)
	fun analyzeClass(input: InputStream, location: String) {
		val buffer: ByteArray
		try {
			buffer = InputStreams.readFully(input)
		} catch (e: IOException) {
			throw analyzerError(location, e)
		}
		analyzeClass(buffer, location)
	}

	/** Creates an [IOException] which includes the affected file location and JaCoCo version.  */
	protected fun analyzerError(
		location: String?,
		cause: Exception?
	): IOException {
		val ex = IOException(
			String.format(
				"Error while analyzing %s with JaCoCo %s/%s.",
				location, JaCoCo.VERSION, JaCoCo.COMMITID_SHORT
			)
		)
		ex.initCause(cause)
		return ex
	}

	/**
	 * Analyzes all classes found in the given input stream. The input stream
	 * may either represent a single class file, a ZIP archive, a Pack200
	 * archive or a gzip stream that is searched recursively for class files.
	 * All other content types are ignored. The provided [InputStream] is
	 * not closed by this method.
	 *
	 * @param input
	 * input data
	 * @param location
	 * a location description used for exception messages
	 * @return number of class files found
	 * @throws IOException
	 * if the stream can't be read or a class can't be analyzed
	 */
	@Throws(IOException::class)
	open fun analyzeAll(input: InputStream, location: String): Int {
		val detector: ContentTypeDetector
		try {
			detector = ContentTypeDetector(input)
		} catch (e: IOException) {
			throw analyzerError(location, e)
		}
		when (detector.type) {
			ContentTypeDetector.CLASSFILE -> {
				analyzeClass(detector.inputStream, location)
				return 1
			}

			ContentTypeDetector.ZIPFILE -> return analyzeZip(detector.inputStream, location)
			ContentTypeDetector.GZFILE -> return analyzeGzip(detector.inputStream, location)
			ContentTypeDetector.PACK200FILE -> return analyzePack200(detector.inputStream, location)
			else -> return 0
		}
	}

	/**
	 * Analyzes all class files contained in the given file or folder. Class
	 * files as well as ZIP files are considered. Folders are searched
	 * recursively.
	 *
	 * @param file
	 * file or folder to look for class files
	 * @return number of class files found
	 * @throws IOException
	 * if the file can't be read or a class can't be analyzed
	 */
	@Throws(IOException::class)
	fun analyzeAll(file: File): Int {
		var count = 0
		if (file.isDirectory) {
			for (f in file.listFiles()) {
				count += analyzeAll(f)
			}
		} else {
			val `in`: InputStream = FileInputStream(file)
			try {
				count += analyzeAll(`in`, file.path)
			} finally {
				`in`.close()
			}
		}
		return count
	}

	/**
	 * Analyzes all classes from the given class path. Directories containing
	 * class files as well as archive files are considered.
	 *
	 * @param path
	 * path definition
	 * @param basedir
	 * optional base directory, if `null` the current
	 * working directory is used as the base for relative path
	 * entries
	 * @return number of class files found
	 * @throws IOException
	 * if a file can't be read or a class can't be analyzed
	 */
	@Throws(IOException::class)
	fun analyzeAll(path: String, basedir: File?): Int {
		var count = 0
		val st = StringTokenizer(
			path,
			File.pathSeparator
		)
		while (st.hasMoreTokens()) {
			count += analyzeAll(File(basedir, st.nextToken()))
		}
		return count
	}

	@Throws(IOException::class)
	private fun analyzeZip(input: InputStream, location: String): Int {
		val zip = ZipInputStream(input)
		var entry: ZipEntry?
		var count = 0
		while ((nextEntry(zip, location).also { entry = it }) != null) {
			count += analyzeAll(zip, location + "@" + entry!!.name)
		}
		return count
	}

	@Throws(IOException::class)
	private fun nextEntry(
		input: ZipInputStream,
		location: String
	): ZipEntry? {
		try {
			return input.nextEntry
		} catch (e: IOException) {
			throw analyzerError(location, e)
		}
	}

	@Throws(IOException::class)
	private fun analyzeGzip(input: InputStream, location: String): Int {
		val gzipInputStream: GZIPInputStream
		try {
			gzipInputStream = GZIPInputStream(input)
		} catch (e: IOException) {
			throw analyzerError(location, e)
		}
		return analyzeAll(gzipInputStream, location)
	}

	@Throws(IOException::class)
	private fun analyzePack200(input: InputStream, location: String): Int {
		val unpackedInput: InputStream
		try {
			unpackedInput = Pack200Streams.unpack(input)
		} catch (e: IOException) {
			throw analyzerError(location, e)
		}
		return analyzeAll(unpackedInput, location)
	}
}
