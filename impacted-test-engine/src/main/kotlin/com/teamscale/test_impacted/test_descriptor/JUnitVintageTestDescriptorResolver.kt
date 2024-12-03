package com.teamscale.test_impacted.test_descriptor

import org.junit.platform.engine.TestDescriptor

/** Test default test descriptor resolver for the JUnit vintage [org.junit.platform.engine.TestEngine].  */
class JUnitVintageTestDescriptorResolver : JUnitClassBasedTestDescriptorResolverBase() {
	override fun getClassName(testDescriptor: TestDescriptor) =
		TestDescriptorUtils.getUniqueIdSegment(testDescriptor, RUNNER_SEGMENT_TYPE)

	override val engineId: String
		get() = "junit-vintage"

	companion object {
		/** The segment type name that the vintage engine uses for the class descriptor nodes.  */
		const val RUNNER_SEGMENT_TYPE = "runner"
	}
}
