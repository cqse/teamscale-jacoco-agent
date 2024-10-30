package com.teamscale.report.testwise

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.teamscale.client.JsonUtils
import com.teamscale.client.StringUtils
import com.teamscale.report.testwise.model.TestInfo
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder
import com.teamscale.report.testwise.model.factory.TestInfoFactory
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.util.function.Consumer

/**
 * Writes out a [com.teamscale.report.testwise.model.TestwiseCoverageReport] one [TestInfo] after the other
 * so that we do not need to keep them all in memory during the conversion.
 */
class TestwiseCoverageReportWriter(
	/** Factory for converting [TestCoverageBuilder] objects to [TestInfo]s.  */
	private val testInfoFactory: TestInfoFactory, private val outputFile: File,
	/** After how many written tests a new file should be started.  */
	private val splitAfter: Int
) : Consumer<TestCoverageBuilder>,
	AutoCloseable {
	/** Writer instance to where the [com.teamscale.report.testwise.model.TestwiseCoverageReport] is written to.  */
	private var jsonGenerator: JsonGenerator? = null

	/** Number of tests written to the file.  */
	private var testsWritten: Int = 0

	/** Number of test files that have been written.  */
	private var testFileCounter: Int = 0

	init {
		startReport()
	}

	override fun accept(testCoverageBuilder: TestCoverageBuilder) {
		val testInfo: TestInfo? = testInfoFactory.createFor(testCoverageBuilder)
		try {
			writeTestInfo(testInfo)
		} catch (e: IOException) {
			// Need to be wrapped in RuntimeException as Consumer does not allow to throw a checked Exception
			throw RuntimeException("Writing test info to report failed.", e)
		}
	}

	@Throws(IOException::class)
	override fun close() {
		for (testInfo: TestInfo? in testInfoFactory.createTestInfosWithoutCoverage()) {
			writeTestInfo(testInfo)
		}
		endReport()
	}

	@Throws(IOException::class)
	private fun startReport() {
		testFileCounter++
		val outputStream: OutputStream = Files.newOutputStream(getOutputFile(testFileCounter).toPath())
		jsonGenerator = JsonUtils.createFactory().createGenerator(outputStream)
		jsonGenerator?.setPrettyPrinter(DefaultPrettyPrinter())
		jsonGenerator?.writeStartObject()
		jsonGenerator?.writeFieldName("tests")
		jsonGenerator?.writeStartArray()
	}

	private fun getOutputFile(testFileCounter: Int): File {
		var name: String = outputFile.getName()
		name = StringUtils.stripSuffix(name, ".json")
		name = name + "-" + testFileCounter + ".json"
		return File(outputFile.getParent(), name)
	}

	@Throws(IOException::class)
	private fun writeTestInfo(testInfo: TestInfo?) {
		if (testsWritten >= splitAfter) {
			endReport()
			testsWritten = 0
			startReport()
		}
		jsonGenerator!!.writeObject(testInfo)
		testsWritten++
	}

	@Throws(IOException::class)
	private fun endReport() {
		jsonGenerator!!.writeEndArray()
		jsonGenerator!!.writeEndObject()
		jsonGenerator!!.close()
	}
}
