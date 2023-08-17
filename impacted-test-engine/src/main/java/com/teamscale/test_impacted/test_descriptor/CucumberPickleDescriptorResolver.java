package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;
import sun.management.Agent;

import java.util.Optional;
import java.util.logging.Logger;

public class CucumberPickleDescriptorResolver extends JUnitTestDescriptorResolverBase {
	@Override
	public String getEngineId() {
		return "junit-platform-suite";
	}

	@Override
	protected Optional<String> getClassName(TestDescriptor testDescriptor) {
		// TODO remove log
		Logger logger = LoggerUtils.getLogger(CucumberPickleDescriptorResolver.class);
		logger.info("Test Descriptor: " + testDescriptor.getDisplayName());
		return Optional.of(testDescriptor.getDisplayName());
	}
}
