package com.teamscale.testimpacted.test_descriptor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

import java.util.Optional;

/** Test default test descriptor resolver for the JUnit vintage {@link TestEngine}. */
public class JUnitVintageTestDescriptorResolver extends JUnitTestDescriptorResolverBase {

	@Override
	protected Optional<String> getClassName(TestDescriptor testDescriptor) {
		return TestDescriptorUtils.getUniqueIdSegment(testDescriptor, "runner");
	}

	@Override
	public String getEngineId() {
		return "junit-vintage";
	}
}
