package com.teamscale.test_impacted.test_descriptor

import com.teamscale.client.ClusteredTestDetails
import com.teamscale.test_impacted.commons.IndentingWriter
import com.teamscale.test_impacted.commons.LoggerUtils.getLogger
import com.teamscale.test_impacted.engine.executor.AvailableTests
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import java.util.*
import java.util.logging.Logger
import java.util.stream.Stream

/** Class containing utility methods for [TestDescriptor]s.  */
object TestDescriptorUtils {
	private val LOGGER = getLogger(TestDescriptorUtils::class.java)

	/** Returns the test descriptor as a formatted string with indented children.  */
	@JvmStatic
	fun getTestDescriptorAsString(testDescriptor: TestDescriptor): String {
		val writer = IndentingWriter()
		printTestDescriptor(writer, testDescriptor)
		return writer.toString()
	}

	private fun printTestDescriptor(writer: IndentingWriter, testDescriptor: TestDescriptor) {
		writer.writeLine(testDescriptor.uniqueId.toString())
		writer.indent {
			testDescriptor.children.forEach { child ->
				printTestDescriptor(writer, child)
			}
		}
	}

	/**
	 * Returns true if the [TestDescriptor] is an actual representative of a test. A representative of a test is
	 * either a regular test that was not dynamically generated or a test container that dynamically registers multiple
	 * test cases.
	 */
	@JvmStatic
	fun isTestRepresentative(testDescriptor: TestDescriptor): Boolean {
		val isTestTemplateOrTestFactory = isTestTemplateOrTestFactory(testDescriptor)
		val isNonParameterizedTest = testDescriptor.isTest && !isTestTemplateOrTestFactory(
			testDescriptor.parent.get()
		)
		return isNonParameterizedTest || isTestTemplateOrTestFactory
	}

	/**
	 * Returns true if a [TestDescriptor] represents a test template or a test factory.
	 *
	 *
	 * An example of a [UniqueId] of the [TestDescriptor] is:
	 *
	 *
	 * `[engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[test-template:withValueSource(java.lang.String)]`
	 */
	private fun isTestTemplateOrTestFactory(testDescriptor: TestDescriptor?): Boolean {
		if (testDescriptor == null) {
			return false
		}
		val segments = testDescriptor.uniqueId.segments

		if (segments.isEmpty()) {
			return false
		}

		val lastSegmentType = segments[segments.size - 1].type
		return JUnitJupiterTestDescriptorResolver.TEST_TEMPLATE_SEGMENT_TYPE == lastSegmentType
				|| JUnitJupiterTestDescriptorResolver.TEST_FACTORY_SEGMENT_TYPE == lastSegmentType
	}

	/** Creates a stream of the test representatives contained by the [TestDescriptor].  */
	private fun streamTestRepresentatives(testDescriptor: TestDescriptor): Stream<TestDescriptor> {
		if (isTestRepresentative(testDescriptor)) {
			return Stream.of(testDescriptor)
		}
		return testDescriptor.children.stream().flatMap {
			streamTestRepresentatives(it)
		}
	}

	/**
	 * Returns the [Segment.getValue] matching the type or [Optional.empty] if no matching segment can
	 * be found.
	 */
	fun getUniqueIdSegment(testDescriptor: TestDescriptor, type: String): Optional<String> =
		testDescriptor.uniqueId.segments.stream()
			.filter { it.type == type }
			.findFirst().map { it.value }

	/** Returns [TestDetails.sourcePath] for a [TestDescriptor].  */
	private fun getSource(testDescriptor: TestDescriptor): String? {
		val source = testDescriptor.source
		if (source.isPresent && source.get() is MethodSource) {
			val ms = source.get() as MethodSource
			return ms.className.replace('.', '/')
		}
		if (source.isPresent && source.get() is ClassSource) {
			val classSource = source.get() as ClassSource
			return classSource.className.replace('.', '/')
		}
		return null
	}

	/** Returns the [AvailableTests] contained within the root [TestDescriptor].  */
	@JvmStatic
	fun getAvailableTests(
		rootTestDescriptor: TestDescriptor,
		partition: String?
	): AvailableTests {
		val availableTests = AvailableTests()

		streamTestRepresentatives(rootTestDescriptor)
			.forEach { testDescriptor: TestDescriptor ->
				val engineId = testDescriptor.uniqueId.engineId
				if (!engineId.isPresent) {
					LOGGER.severe { "Unable to determine engine ID for $testDescriptor!" }
					return@forEach
				}

				val testDescriptorResolver = TestDescriptorResolverRegistry.getTestDescriptorResolver(engineId.get())
				val clusterId = testDescriptorResolver!!.getClusterId(testDescriptor)
				val uniformPath = testDescriptorResolver.getUniformPath(testDescriptor)
				val source = getSource(testDescriptor)

				if (!uniformPath.isPresent) {
					LOGGER.severe { "Unable to determine uniform path for test descriptor: $testDescriptor" }
					return@forEach
				}

				if (!clusterId.isPresent) {
					LOGGER.severe { "Unable to determine cluster id path for test descriptor: $testDescriptor" }
					return@forEach
				}

				val testDetails = ClusteredTestDetails(
					uniformPath.get(), source, null,
					clusterId.get(), partition
				)
				availableTests.add(testDescriptor.uniqueId, testDetails)
			}


		return availableTests
	}
}
