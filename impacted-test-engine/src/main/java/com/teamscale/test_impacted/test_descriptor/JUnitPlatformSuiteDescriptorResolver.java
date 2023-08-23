package com.teamscale.test_impacted.test_descriptor;

import org.junit.platform.engine.TestDescriptor;

import java.util.Optional;

public class JUnitPlatformSuiteDescriptorResolver implements ITestDescriptorResolver {
	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		// Delegate to nested engines via the TestDescriptorResolverRegistry like this:
		// return Optional.of("suite-name/" + TestDescriptorResolverRegistry.getTestDescriptorResolver(engineID).getUniformPath());
		return Optional.empty();
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		// Delegate to nested engines via the TestDescriptorResolverRegistry like this:
		// return Optional.of(TestDescriptorResolverRegistry.getTestDescriptorResolver(engineID).getUniformPath());
		return Optional.empty();
	}

	@Override
	public String getEngineId() {
		return "junit-platform-suite";
	}
}
