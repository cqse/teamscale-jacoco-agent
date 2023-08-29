package com.teamscale.test_impacted.test_descriptor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CucumberPickleDescriptorResolver implements ITestDescriptorResolver {
	public static final String CUCUMBER_ENGINE_ID = "cucumber";

	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		List<UniqueId.Segment> cucumberSegments = getCucumberSegments(testDescriptor);
		return Optional.of(transformSegmentsToTeamscalePath(cucumberSegments));
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		List<UniqueId.Segment> cucumberSegments = getCucumberSegments(testDescriptor);
		return Optional.of(transformSegmentsToTeamscalePath(cucumberSegments));
	}

	@Override
	public String getEngineId() {
		return CUCUMBER_ENGINE_ID;
	}

	private List<UniqueId.Segment> getCucumberSegments(TestDescriptor testDescriptor) {
		List<UniqueId.Segment> allSegments = testDescriptor.getUniqueId().getSegments();
		UniqueId.Segment cucumberEngineSegment = allSegments.stream()
				.filter(segment -> segment.getType().equals(ENGINE_SEGMENT_TYPE) && segment.getValue()
						.equals(CUCUMBER_ENGINE_ID))
				.findFirst()
				.get(); // We know that we get a segment with "engine" from either the TestDescriptorResolverRegistry or the JUnitPlatformSuiteDescriptorResolver
		int indexOfCucumberEngineSegment = allSegments.indexOf(cucumberEngineSegment);
		return allSegments.subList(indexOfCucumberEngineSegment + 1, allSegments.size());
	}

	/**
	 * Transform unique id segments from something like
	 * [feature:classpath%3Ahellocucumber%2Fcalculator.feature]/[scenario:11]/[examples:16]/[example:21] to
	 * feature-hellocucumber.calculator.feature/scenario-11/examples-16/example-21
	 */
	private static String transformSegmentsToTeamscalePath(List<UniqueId.Segment> cucumberSegments) {
		List<String> normalizedSegments = cucumberSegments.stream()
				.map(segment -> segment.getType() + "-" + segment.getValue())
				.map(uniformPathSegment -> uniformPathSegment.replaceAll("classpath:", "").replaceAll("/", "."))
				.collect(Collectors.toList());
		return String.join("/", normalizedSegments);
	}
}
