package com.teamscale.test_impacted.test_descriptor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.TestDetails;
import com.teamscale.test_impacted.commons.IndentingWriter;
import com.teamscale.test_impacted.engine.executor.AvailableTests;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.support.descriptor.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Class containing utility methods for {@link TestDescriptor}s. */
public class TestDescriptorUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDescriptorUtils.class);

	/** Returns the test descriptor as a formatted string with indented children. */
	public static String getTestDescriptorAsString(TestDescriptor testDescriptor) {
		IndentingWriter writer = new IndentingWriter();
		printTestDescriptor(writer, testDescriptor);
		return writer.toString();
	}

	private static void printTestDescriptor(IndentingWriter writer, TestDescriptor testDescriptor) {
		writer.writeLine(testDescriptor.getUniqueId().toString());
		writer.indent(() -> {
			for (TestDescriptor child : testDescriptor.getChildren()) {
				printTestDescriptor(writer, child);
			}
		});
	}

	/**
	 * Returns true if the {@link TestDescriptor} is an actual representative of a test. A representative of a test is
	 * either a regular test that was not dynamically generated or a test container that dynamically registers multiple
	 * test cases.
	 */
	public static boolean isTestRepresentative(TestDescriptor testDescriptor) {
		boolean isTestTemplateOrTestFactory = isTestTemplateOrTestFactory(testDescriptor);
		boolean isNonParameterizedTest = testDescriptor.isTest() && !isTestTemplateOrTestFactory(
				testDescriptor.getParent().get());
		return isNonParameterizedTest || isTestTemplateOrTestFactory;
	}

	/**
	 * Returns true if a {@link TestDescriptor} represents a test template or a test factory.
	 * <p>
	 * An example of a {@link UniqueId} of the {@link TestDescriptor} is:
	 * <p>
	 * {@code [engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[test-template:withValueSource(java.lang.String)]}
	 */
	private static boolean isTestTemplateOrTestFactory(TestDescriptor testDescriptor) {
		if (testDescriptor == null) {
			return false;
		}
		List<Segment> segments = testDescriptor.getUniqueId().getSegments();

		if (segments.isEmpty()) {
			return false;
		}

		String lastSegmentType = segments.get(segments.size() - 1).getType();
		return "test-template".equals(lastSegmentType) || "test-factory".equals(lastSegmentType);
	}

	/** Creates a stream of the test representatives contained by the {@link TestDescriptor}. */
	public static Stream<TestDescriptor> streamTestRepresentatives(TestDescriptor testDescriptor) {
		if (isTestRepresentative(testDescriptor)) {
			return Stream.of(testDescriptor);
		}
		return testDescriptor.getChildren().stream().flatMap(TestDescriptorUtils::streamTestRepresentatives);
	}

	/**
	 * Returns the {@link Segment#getValue()} matching the type or {@link Optional#empty()} if no matching segment can
	 * be found.
	 */
	public static Optional<String> getUniqueIdSegment(TestDescriptor testDescriptor, String type) {
		return testDescriptor.getUniqueId().getSegments().stream().filter(segment -> segment.getType().equals(type))
				.findFirst().map(
						Segment::getValue);
	}

	/** Returns {@link TestDetails#sourcePath} for a {@link TestDescriptor}. */
	public static String getSource(TestDescriptor testDescriptor) {
		Optional<TestSource> source = testDescriptor.getSource();
		if (source.isPresent() && source.get() instanceof MethodSource) {
			MethodSource ms = (MethodSource) source.get();
			return ms.getClassName().replace('.', '/');
		}
		return null;
	}

	/** Returns the {@link AvailableTests} contained within the root {@link TestDescriptor}. */
	public static AvailableTests getAvailableTests(TestEngine testEngine, TestDescriptor rootTestDescriptor) {
		AvailableTests availableTests = new AvailableTests();
		ITestDescriptorResolver testDescriptorResolver = TestDescriptorResolverRegistry
				.getTestDescriptorResolver(testEngine);

		TestDescriptorUtils.streamTestRepresentatives(rootTestDescriptor)
				.forEach(testDescriptor -> {
					Optional<String> clusterId = testDescriptorResolver.getClusterId(testDescriptor);
					Optional<String> uniformPath = testDescriptorResolver.getUniformPath(testDescriptor);
					String source = TestDescriptorUtils.getSource(testDescriptor);

					if (!uniformPath.isPresent()) {
						LOGGER.error(() -> "Unable to determine uniform path for test descriptor: " + testDescriptor);
						return;
					}

					if (!clusterId.isPresent()) {
						LOGGER.error(
								() -> "Unable to determine cluster id path for test descriptor: " + testDescriptor);
						return;
					}

					ClusteredTestDetails testDetails = new ClusteredTestDetails(uniformPath.get(), source, null,
							clusterId.get());
					availableTests.add(testDescriptor.getUniqueId(), testDetails);
				});


		return availableTests;
	}
}
