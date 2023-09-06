package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

import java.util.Optional;
import java.util.logging.Logger;

/** Test descriptor resolver for JUnit based {@link TestEngine}s. */
public abstract class JUnitClassBasedTestDescriptorResolverBase implements ITestDescriptorResolver {

	private static final Logger LOGGER = LoggerUtils.getLogger(JUnitClassBasedTestDescriptorResolverBase.class);

	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		return getClassName(testDescriptor).map(className -> {
			String classNameUniformPath = className.replace(".", "/");
			return classNameUniformPath + "/" + testDescriptor.getLegacyReportingName().trim();
		});
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		Optional<String> classSegmentName = getClassName(testDescriptor);

		if (!classSegmentName.isPresent()) {
			LOGGER.severe(
					() -> "Falling back to unique ID as cluster id because class segment name could not be " +
							"determined for test descriptor: " + testDescriptor);
			// Default to uniform path.
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		return classSegmentName;
	}

	/** Returns the test class containing the test. */
	protected abstract Optional<String> getClassName(TestDescriptor testDescriptor);

}
