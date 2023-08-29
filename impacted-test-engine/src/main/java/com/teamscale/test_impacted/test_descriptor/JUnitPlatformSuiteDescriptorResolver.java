package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class JUnitPlatformSuiteDescriptorResolver implements ITestDescriptorResolver {

	private static final Logger LOGGER = LoggerUtils.getLogger(JUnitPlatformSuiteDescriptorResolver.class);
	public static final String SUITE_SEGMENT_TYPE = "suite";

	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		LOGGER.info(testDescriptor.toString());
		List<UniqueId.Segment> segments = testDescriptor.getUniqueId().getSegments();
		if (verifySegments(segments)) {
			LOGGER.severe(
					() -> "Assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine] " +
							"for junit-platform-suite tests. Using "
							+ testDescriptor.getUniqueId()
							.toString() + "as uniform path as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		UniqueId.Segment suiteSegment = segments.get(1);
		List<UniqueId.Segment> secondaryEngineSegments = segments.subList(2, segments.size());

		ITestDescriptorResolver secondaryTestDescriptorResolver = TestDescriptorResolverRegistry.getTestDescriptorResolver(
				secondaryEngineSegments.get(0).getValue());
		if (secondaryTestDescriptorResolver == null) {
			LOGGER.severe(() -> "Cannot find a secondary engine nested under the junit-platform-suite engine " +
					"(assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine])" +
					"Using " + testDescriptor.getUniqueId().toString() + "as uniform path as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		Optional<String> secondaryUniformPath = secondaryTestDescriptorResolver.getUniformPath(testDescriptor);
		if (!secondaryUniformPath.isPresent()) {
			LOGGER.severe(() -> "Secondary test descriptor resolver for engine " +
					secondaryEngineSegments.get(0).getValue() + " was not able to resolve the uniform path. " +
					"Using " + testDescriptor.getUniqueId().toString() + "as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		LOGGER.info(suiteSegment.getValue() + "/" + secondaryUniformPath.get());
		return Optional.of(suiteSegment.getValue() + "/" + secondaryUniformPath.get());
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		List<UniqueId.Segment> segments = testDescriptor.getUniqueId().getSegments();
		if (verifySegments(segments)) {
			LOGGER.severe(
					() -> "Assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine] " +
							"for junit-platform-suite tests. Using "
							+ testDescriptor.getUniqueId()
							.toString() + "as cluster id as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		UniqueId.Segment suiteSegment = segments.get(1);
		List<UniqueId.Segment> secondaryEngineSegments = segments.subList(2, segments.size());

		ITestDescriptorResolver secondaryTestDescriptorResolver = TestDescriptorResolverRegistry.getTestDescriptorResolver(
				secondaryEngineSegments.get(0).getValue());
		if (secondaryTestDescriptorResolver == null) {
			LOGGER.severe(() -> "Cannot find a secondary engine nested under the junit-platform-suite engine " +
					"(assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine])" +
					"Using " + testDescriptor.getUniqueId().toString() + "as cluster id as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		Optional<String> secondaryClusterId = secondaryTestDescriptorResolver.getClusterId(testDescriptor);
		if (!secondaryClusterId.isPresent()) {
			LOGGER.severe(() -> "Secondary test descriptor resolver for engine " +
					secondaryEngineSegments.get(0).getValue() + " was not able to resolve the cluster id. " +
					"Using " + testDescriptor.getUniqueId().toString() + "as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}
		return Optional.of(suiteSegment.getValue() + "/" + secondaryClusterId.get());
	}

	private static boolean verifySegments(List<UniqueId.Segment> segments) {
		return segments.size() < 3 || !segments.get(0).getType()
				.equals(ITestDescriptorResolver.ENGINE_SEGMENT_TYPE) || !segments.get(1)
				.getType()
				.equals(SUITE_SEGMENT_TYPE) || !segments.get(2).getType().equals(
				ITestDescriptorResolver.ENGINE_SEGMENT_TYPE);
	}

	@Override
	public String getEngineId() {
		return "junit-platform-suite";
	}
}
