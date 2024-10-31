/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package com.teamscale.report.jacoco

import com.teamscale.report.util.BashFileSkippingInputStream
import com.teamscale.report.util.ClasspathWildcardIncludeFilter
import com.teamscale.report.util.ILogger
import com.teamscale.report.util.error
import org.jacoco.core.analysis.ICoverageVisitor
import org.jacoco.core.data.ExecutionDataStore
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * [Analyzer] that filters the analyzed class files based on a [Predicate].
 */
/* package */
open class FilteringAnalyzer(
	executionData: ExecutionDataStore?,
	coverageVisitor: ICoverageVisitor?,
	/** The filter for the analyzed class files.  */
	private val locationIncludeFilter: ClasspathWildcardIncludeFilter,
	private val logger: ILogger
) : OpenAnalyzer(executionData, coverageVisitor) {
	/** {@inheritDoc}  */
	@Throws(IOException::class)
	override fun analyzeAll(input: InputStream, location: String): Int {
		if (location.endsWith(".class") && !locationIncludeFilter.isIncluded(location)) {
			logger.debug("Excluding class file $location")
			return 1
		}
		if (location.endsWith(".jar")) {
			return analyzeJar(input, location)
		}
		return super.analyzeAll(input, location)
	}

	@Throws(IOException::class)
	override fun analyzeClass(buffer: ByteArray, location: String) {
		try {
			analyzeClass(buffer)
		} catch (cause: RuntimeException) {
			if (isUnsupportedClassFile(cause)) {
				logger.error(cause.message + " in " + location)
			} else {
				throw analyzerError(location, cause)
			}
		}
	}

	/**
	 * Checks if the error indicates that the class file might be newer than what is currently supported by
	 * JaCoCo. The concrete error message seems to depend on the used JVM, so we only check for "Unsupported" which seems
	 * to be common amongst all of them.
	 */
	private fun isUnsupportedClassFile(cause: RuntimeException): Boolean {
		return cause is IllegalArgumentException && cause.message?.startsWith("Unsupported") == true
	}

	/**
	 * Copied from Analyzer.analyzeZip renamed to analyzeJar and added wrapping BashFileSkippingInputStream.
	 */
	@Throws(IOException::class)
	protected open fun analyzeJar(input: InputStream, location: String): Int {
		val zip = ZipInputStream(BashFileSkippingInputStream(input))
		var entry: ZipEntry?
		var count = 0
		while ((nextEntry(zip, location).also { entry = it }) != null) {
			count += analyzeAll(zip, location + "@" + entry!!.name)
		}
		return count
	}

	/** Copied from Analyzer.nextEntry.  */
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
}
