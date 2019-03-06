package com.teamscale.testimpacted.test_descriptor;

import com.teamscale.testimpacted.junit.executor.AvailableTests;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

public class TestDescriptorUtils {

	private static class IndentingWriter {

		private StringBuilder builder = new StringBuilder();

		private int indent = 0;

		private void indent(Runnable indentedWrites) {
			indent++;
			indentedWrites.run();
			indent--;
		}

		private void writeLine(String line) {
			for (int i = 0; i < indent; i++) {
				builder.append("\t");
			}
			builder.append(line).append("\n");
		}

		@Override
		public String toString() {
			return builder.toString();
		}
	}

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


	public static boolean isRelevantTestInstance(TestDescriptor testDescriptor) {
		boolean isParameterizedTestContainer = testDescriptor.isContainer() && containsParameterizedTestContainer(
				testDescriptor);
		boolean isNonParameterizedTest = testDescriptor.isTest() && !containsParameterizedTestContainer(testDescriptor);
		return isNonParameterizedTest || isParameterizedTestContainer;
	}

	/**
	 * Looks like this: [engine:junit-jupiter]/[class:com.example.project.JUnit5Test]/[test-template:withValueSource(java.lang.String)]
	 */
	private static boolean containsParameterizedTestContainer(TestDescriptor testDescriptor) {
		return testDescriptor.getUniqueId().getSegments().stream().map(UniqueId.Segment::getType)
				.anyMatch("test-template"::equals);
	}


	public static Stream<TestDescriptor> streamRelevantTestDescriptors(TestDescriptor testDescriptor) {
		if (isRelevantTestInstance(testDescriptor)) {
			return Stream.of(testDescriptor);
		}
		return testDescriptor.getChildren().stream().flatMap(TestDescriptorUtils::streamRelevantTestDescriptors);
	}

	public static Optional<String> getUniqueIdSegment(TestDescriptor testDescriptor, String type) {
		return testDescriptor.getUniqueId().getSegments().stream().filter(segment -> segment.getType().equals(type))
				.findFirst().map(
						UniqueId.Segment::getValue);
	}

	public static String getSource(TestDescriptor testDescriptor) {
		Optional<TestSource> source = testDescriptor.getSource();
		if (source.isPresent() && source.get() instanceof MethodSource) {
			MethodSource ms = (MethodSource) source.get();
			return ms.getClassName().replace('.', '/');
		}
		return null;
	}

	public static AvailableTests getAvailableTests(TestEngine testEngine, TestDescriptor rootTestDescriptor) {
		AvailableTests availableTests = new AvailableTests();
		ITestDescriptorResolver testDescriptorResolver = TestDescriptorResolverRegistry
				.getTestDescriptorResolver(testEngine);

		TestDescriptorUtils.streamRelevantTestDescriptors(rootTestDescriptor)
				.forEach(testDescriptor ->
						testDescriptorResolver
								.toClusteredTestDetails(testDescriptor)
								.ifPresent(clusteredTestDetails ->
										availableTests.add(testDescriptor.getUniqueId(), clusteredTestDetails)
								));


		return availableTests;
	}
}
