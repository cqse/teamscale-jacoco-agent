package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.test_impacted.commons.LoggerUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CucumberPickleDescriptorResolver implements ITestDescriptorResolver {
	private static final Logger LOGGER = LoggerUtils.getLogger(JUnitPlatformSuiteDescriptorResolver.class);

	@Override
	public Optional<String> getUniformPath(TestDescriptor testDescriptor) {
		LOGGER.info("Resolving cucumber uniform path");
		List<UniqueId.Segment> cucumberSegments = getCucumberSegments(testDescriptor);
		List<String> uniformPathSegments = cucumberSegments.stream()
				.map(segment -> segment.getType() + "-" + segment.getValue())
				.map(uniformPathSegment -> uniformPathSegment.replaceAll("classpath:", "").replaceAll("/", "."))
				.collect(Collectors.toList());
		return Optional.of(String.join("/", uniformPathSegments));
	}

	@Override
	public Optional<String> getClusterId(TestDescriptor testDescriptor) {
		LOGGER.info("Resolving cucumber cluster id");
		List<UniqueId.Segment> cucumberSegments = getCucumberSegments(testDescriptor);
		List<String> uniformPathSegments = cucumberSegments.stream().map(UniqueId.Segment::getValue)
				.collect(Collectors.toList());
		return Optional.of(String.join("/", uniformPathSegments));
	}

	@Override
	public String getEngineId() {
		return "cucumber";
	}

	private List<UniqueId.Segment> getCucumberSegments(TestDescriptor testDescriptor) {
		List<UniqueId.Segment> allSegments = testDescriptor.getUniqueId().getSegments();
		// TODO replace strings with constants
		UniqueId.Segment cucumberEngineSegment = allSegments.stream()
				.filter(segment -> segment.getType().equals("engine") && segment.getValue().equals("cucumber"))
				.findFirst().get();
		int indexOfCucumberEngineSegment = allSegments.indexOf(cucumberEngineSegment);
		return allSegments.subList(indexOfCucumberEngineSegment + 1, allSegments.size());
	}
}
