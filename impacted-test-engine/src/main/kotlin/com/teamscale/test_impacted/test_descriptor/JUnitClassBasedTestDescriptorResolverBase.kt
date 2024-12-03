package com.teamscale.test_impacted.test_descriptor

import com.teamscale.test_impacted.commons.LoggerUtils.getLogger
import org.junit.platform.engine.TestDescriptor
import java.util.*

/** Test descriptor resolver for JUnit based [org.junit.platform.engine.TestEngine]s.  */
abstract class JUnitClassBasedTestDescriptorResolverBase : ITestDescriptorResolver {
	override fun getUniformPath(testDescriptor: TestDescriptor): Optional<String> =
		getClassName(testDescriptor).map { className ->
			val dotName = className.replace(".", "/")
			"$dotName/${testDescriptor.legacyReportingName.trim { it <= ' ' }}"
		}

	override fun getClusterId(testDescriptor: TestDescriptor): Optional<String> {
		val classSegmentName = getClassName(testDescriptor)

		if (!classSegmentName.isPresent) {
			LOGGER.severe {
				"Falling back to unique ID as cluster id because class segment name could not be " +
						"determined for test descriptor: " + testDescriptor
			}
			// Default to uniform path.
			return Optional.of(testDescriptor.uniqueId.toString())
		}

		return classSegmentName
	}

	/** Returns the test class containing the test.  */
	protected abstract fun getClassName(testDescriptor: TestDescriptor): Optional<String>

	companion object {
		private val LOGGER = getLogger(JUnitClassBasedTestDescriptorResolverBase::class.java)
	}
}
