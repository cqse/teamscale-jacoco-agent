package com.teamscale.test_impacted.test_descriptor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

import java.util.Optional;

/** Test default test descriptor resolver for the JUnit jupiter {@link TestEngine}. */
public class JUnitJupiterTestDescriptorResolver extends JUnitClassBasedTestDescriptorResolverBase {

	/** The segment type name that the jupiter engine uses for the class descriptor nodes. */
	public static final String CLASS_SEGMENT_TYPE = "class";

	/** The segment type name that the jupiter engine uses for the method descriptor nodes. */
	public static final String METHOD_SEGMENT_TYPE = "method";

	/** The segment type name that the jupiter engine uses for the test factory method descriptor nodes. */
	public static final String TEST_FACTORY_SEGMENT_TYPE = "test-factory";

	/** The segment type name that the jupiter engine uses for the test template descriptor nodes. */
	public static final String TEST_TEMPLATE_SEGMENT_TYPE = "test-template";

	/** The segment type name that the jupiter engine uses for dynamic test descriptor nodes. */
	public static final String DYNAMIC_TEST_SEGMENT_TYPE = "dynamic-test";

	@Override
	protected Optional<String> getClassName(TestDescriptor testDescriptor) {
		return TestDescriptorUtils.getUniqueIdSegment(testDescriptor, CLASS_SEGMENT_TYPE);
	}

	@Override
	public String getEngineId() {
		return "junit-jupiter";
	}
}
