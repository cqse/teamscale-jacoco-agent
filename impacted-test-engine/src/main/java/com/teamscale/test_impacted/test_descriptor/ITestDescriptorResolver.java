package com.teamscale.test_impacted.test_descriptor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

import java.util.Optional;

/** Interface for implementation of mappings from {@link TestDescriptor}s to uniform paths. */
public interface ITestDescriptorResolver {

	/** Type of the unique id segment of a test descriptor representing a test engine */
	String ENGINE_SEGMENT_TYPE = "engine";

	/** Returns the uniform path or {@link Optional#empty()} if no uniform path could be determined. */
	Optional<String> getUniformPath(TestDescriptor testDescriptor);

	/** Returns the uniform path or {@link Optional#empty()} if no cluster id could be determined. */
	Optional<String> getClusterId(TestDescriptor testDescriptor);

	/**
	 * Returns the {@link TestEngine#getId()} of the {@link TestEngine} to use this {@link ITestDescriptorResolver}
	 * for.
	 */
	String getEngineId();
}
