package com.teamscale.test_impacted.test_descriptor

import com.teamscale.test_impacted.test_descriptor.TestDescriptorUtils.getUniqueIdSegment
import org.junit.platform.engine.TestDescriptor
import java.util.*

/** Test default test descriptor resolver for the JUnit jupiter [TestEngine].  */
class JUnitJupiterTestDescriptorResolver : JUnitClassBasedTestDescriptorResolverBase() {
	override fun TestDescriptor.getClassName() =
		getUniqueIdSegment(CLASS_SEGMENT_TYPE)

	override val engineId: String
		get() = "junit-jupiter"

	companion object {
		/** The segment type name that the jupiter engine uses for the class descriptor nodes.  */
		const val CLASS_SEGMENT_TYPE = "class"

		/** The segment type name that the jupiter engine uses for the method descriptor nodes.  */
		const val METHOD_SEGMENT_TYPE = "method"

		/** The segment type name that the jupiter engine uses for the test factory method descriptor nodes.  */
		const val TEST_FACTORY_SEGMENT_TYPE = "test-factory"

		/** The segment type name that the jupiter engine uses for the test template descriptor nodes.  */
		const val TEST_TEMPLATE_SEGMENT_TYPE = "test-template"

		/** The segment type name that the jupiter engine uses for dynamic test descriptor nodes.  */
		const val DYNAMIC_TEST_SEGMENT_TYPE = "dynamic-test"
	}
}
