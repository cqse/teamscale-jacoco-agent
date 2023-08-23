package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;
import sun.management.Agent;

import java.util.Optional;
import java.util.logging.Logger;

public class CucumberPickleDescriptorResolver implements ITestDescriptorResolver {
	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		return Optional.empty();
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		return Optional.empty();
	}

	@Override
	public String getEngineId() {
		return "cucumber";
	}
}
