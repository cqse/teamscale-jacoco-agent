package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Test descriptor resolver for JUnit Platform Suite test (c.f.
 * https://junit.org/junit5/docs/current/user-guide/#junit-platform-suite-engine)
 */
public class JUnitPlatformSuiteDescriptorResolver implements ITestDescriptorResolver {

	private static final Logger LOGGER = LoggerUtils.getLogger(JUnitPlatformSuiteDescriptorResolver.class);
	/** Type of the unique id segment of a test descriptor representing a test suite */
	public static final String SUITE_SEGMENT_TYPE = "suite";

	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		return extractUniformPathOrClusterId(testDescriptor, "uniform path",
				testDescriptorResolver -> testDescriptorResolver.getUniformPath(testDescriptor));
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		return extractUniformPathOrClusterId(testDescriptor, "cluster id",
				testDescriptorResolver -> testDescriptorResolver.getClusterId(testDescriptor));
	}

	private static Optional<String> extractUniformPathOrClusterId(TestDescriptor testDescriptor,
																  String nameOfValueToExtractForLogs,
																  Function<ITestDescriptorResolver, Optional<String>> uniformPathOrClusterIdExtractor) {
		List<UniqueId.Segment> segments = testDescriptor.getUniqueId().getSegments();
		if (verifySegments(segments)) {
			LOGGER.severe(
					() -> "Assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine] " +
							"for junit-platform-suite tests. Using "
							+ testDescriptor.getUniqueId()
							.toString() + "as " + nameOfValueToExtractForLogs + " as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		String suite = segments.get(1).getValue().replace('.', '/');
		List<UniqueId.Segment> secondaryEngineSegments = segments.subList(2, segments.size());

		ITestDescriptorResolver secondaryTestDescriptorResolver = TestDescriptorResolverRegistry.getTestDescriptorResolver(
				secondaryEngineSegments.get(0).getValue());
		if (secondaryTestDescriptorResolver == null) {
			LOGGER.severe(() -> "Cannot find a secondary engine nested under the junit-platform-suite engine " +
					"(assuming structure [engine:junit-platform-suite]/[suite:mySuite]/[engine:anotherEngine])" +
					"Using " + testDescriptor.getUniqueId()
					.toString() + "as " + nameOfValueToExtractForLogs + " as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		Optional<String> secondaryClusterIdOrUniformPath = uniformPathOrClusterIdExtractor.apply(
				secondaryTestDescriptorResolver);
		if (!secondaryClusterIdOrUniformPath.isPresent()) {
			LOGGER.severe(() -> "Secondary test descriptor resolver for engine " +
					secondaryEngineSegments.get(0)
							.getValue() + " was not able to resolve the " + nameOfValueToExtractForLogs + ". " +
					"Using " + testDescriptor.getUniqueId().toString() + "as fallback.");
			return Optional.of(testDescriptor.getUniqueId().toString());
		}

		return Optional.of(suite + "/" + secondaryClusterIdOrUniformPath.get());
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
