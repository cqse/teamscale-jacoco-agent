package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class JUnitPlatformSuiteDescriptorResolver implements ITestDescriptorResolver {

	private static final Logger LOGGER = LoggerUtils.getLogger(JUnitPlatformSuiteDescriptorResolver.class);

	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		// Delegate to nested engines via the TestDescriptorResolverRegistry like this:
		// return Optional.of(TestDescriptorResolverRegistry.getTestDescriptorResolver(engineID).getUniformPath());
		// TODO can we remove the clone here?
		LOGGER.info(testDescriptor.toString());
		List<UniqueId.Segment> segments = testDescriptor.getUniqueId().getSegments();
		// TODO refactor stings to constants (or reuse the ones we already have)
		if (segments.size() < 3 || !segments.get(0).getType().equals("engine") || !segments.get(1).getType()
				.equals("suite") || !segments.get(2).getType().equals("engine")) {
			// TODO Do we actually want to use this default or do we want to crash?
			LOGGER.severe(() ->
					"Assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine] " +
							"for junit-platform-suite tests. Using "
							+ testDescriptor.getUniqueId()
							.toString() + "as uniform path as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		UniqueId.Segment suiteSegment = segments.get(1);
		List<UniqueId.Segment> secondaryEngineSegments = segments.subList(2, segments.size());

		if (secondaryEngineSegments.isEmpty() || !secondaryEngineSegments.get(0).getType().equals("engine")) {
			// TODO log error
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		ITestDescriptorResolver secondaryTestDescriptorResolver = TestDescriptorResolverRegistry.getTestDescriptorResolver(
				secondaryEngineSegments.get(0).getValue());
		if (secondaryTestDescriptorResolver == null) {
			// TODO log error
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		// TODO make existing testDescriptors work if they are only secondary resolvers (maybe they do already)
		Optional<String> secondaryUniformPath = secondaryTestDescriptorResolver.getUniformPath(testDescriptor);
		if (!secondaryUniformPath.isPresent()) {
			// TODO log error
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		LOGGER.info(suiteSegment.getValue() + "/" + secondaryUniformPath.get());
		return Optional.of(suiteSegment.getValue() + "/" +
				secondaryUniformPath.get());
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		// Delegate to nested engines via the TestDescriptorResolverRegistry like this:
		// return Optional.of(TestDescriptorResolverRegistry.getTestDescriptorResolver(engineID).getUniformPath());
		List<UniqueId.Segment> segments = testDescriptor.getUniqueId().getSegments();
		// TODO refactor stings to constants (or reuse the ones we already have)
		if (segments.size() < 3 || !segments.get(0).getType().equals("engine") || !segments.get(1).getType()
				.equals("suite") || !segments.get(2).getType().equals("engine")) {
			// TODO Do we actually want to use this default or do we want to crash?
			LOGGER.severe(() ->
					"Assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine] " +
							"for junit-platform-suite tests. Using "
							+ testDescriptor.getUniqueId()
							.toString() + "as uniform path as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		UniqueId.Segment suiteSegment = segments.get(1);
		List<UniqueId.Segment> secondaryEngineSegments = segments.subList(2, segments.size());

		if (secondaryEngineSegments.isEmpty() || !secondaryEngineSegments.get(0).getType().equals("engine")) {
			// TODO log error
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		ITestDescriptorResolver secondaryTestDescriptorResolver = TestDescriptorResolverRegistry.getTestDescriptorResolver(
				secondaryEngineSegments.get(0).getValue());
		if (secondaryTestDescriptorResolver == null) {
			// TODO log error
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		Optional<String> secondaryClusterId = secondaryTestDescriptorResolver.getClusterId(testDescriptor);
		if (!secondaryClusterId.isPresent()) {
			// TODO log error
			return Optional.of(testDescriptor.getUniqueId().toString());
		}
		return Optional.of(suiteSegment.getValue() + "/" +
				secondaryClusterId.get());
	}

	@Override
	public String getEngineId() {
		return "junit-platform-suite";
	}
}
