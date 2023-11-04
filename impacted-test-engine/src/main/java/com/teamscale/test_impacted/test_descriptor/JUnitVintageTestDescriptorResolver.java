package com.teamscale.test_impacted.test_descriptor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

import java.util.Optional;

/** Test default test descriptor resolver for the JUnit vintage {@link TestEngine}. */
public class JUnitVintageTestDescriptorResolver extends JUnitClassBasedTestDescriptorResolverBase {

	/** The segment type name that the vintage engine uses for the class descriptor nodes. */
	public static final String RUNNER_SEGMENT_TYPE = "runner";

	@Override
	protected Optional<String> getClassName(TestDescriptor testDescriptor) {
		return TestDescriptorUtils.getUniqueIdSegment(testDescriptor, RUNNER_SEGMENT_TYPE);
	}

	@Override
	public String getEngineId() {
		return "junit-vintage";
	}
}
