package com.teamscale.testimpacted.test_descriptor;

import org.junit.platform.engine.TestDescriptor;

import java.util.Optional;

public class JUnitVintageTestDescriptorResolver extends JUnitTestDescriptorResolverBase {

	protected Optional<String> getClassSegment(TestDescriptor testDescriptor) {
		return TestDescriptorUtils.getUniqueIdSegment(testDescriptor, "runner");
	}
}
