package com.teamscale.report.testwise.model.builder;

import com.teamscale.client.TestDetails;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Container for coverage produced by multiple tests. */
public class TestwiseCoverageReportBuilder {

	/** A mapping from test ID to {@link TestCoverageBuilder}. */
	private final Map<String, TestInfoBuilder> tests = new HashMap<>();

	/**
	 * Adds the {@link TestCoverageBuilder} to the map. If there is already a test with the same ID the coverage is
	 * merged.
	 */
	public static TestwiseCoverageReport createFrom(
			Collection<TestDetails> testDetailsList,
			Collection<TestCoverageBuilder> testCoverage,
			Collection<TestExecution> testExecutions
	) {
		TestwiseCoverageReportBuilder report = new TestwiseCoverageReportBuilder();
		for (TestDetails testDetails : testDetailsList) {
			TestInfoBuilder container = new TestInfoBuilder(testDetails.uniformPath);
			container.setDetails(testDetails);
			report.tests.put(testDetails.uniformPath, container);
		}
		for (TestCoverageBuilder coverage : testCoverage) {
			TestInfoBuilder container = resolveUniformPath(report, coverage.getUniformPath());
			if (container == null) {
				continue;
			}
			container.setCoverage(coverage);
		}
		for (TestExecution testExecution : testExecutions) {
			TestInfoBuilder container = resolveUniformPath(report, testExecution.getUniformPath());
			if (container == null) {
				continue;
			}
			container.setExecution(testExecution);
		}
		return report.build();
	}

	private static TestInfoBuilder resolveUniformPath(TestwiseCoverageReportBuilder report, String uniformPath) {
		TestInfoBuilder container = report.tests.get(uniformPath);
		if (container != null) {
			return container;
		}
		String shortenedUniformPath = uniformPath.replaceFirst("(.*\\))\\[.*]", "$1");
		TestInfoBuilder testInfoBuilder = report.tests.get(shortenedUniformPath);
		if (testInfoBuilder == null) {
			System.err.println("No container found for test '" + uniformPath + "'!");
		}
		return testInfoBuilder;
	}

	private TestwiseCoverageReport build() {
		TestwiseCoverageReport report = new TestwiseCoverageReport();
		List<TestInfoBuilder> testInfoBuilders = new ArrayList<>(tests.values());
		testInfoBuilders.sort(Comparator.comparing(TestInfoBuilder::getUniformPath));
		for (TestInfoBuilder testInfoBuilder : testInfoBuilders) {
			TestInfo testInfo = testInfoBuilder.build();
			if (testInfo == null) {
				System.err.println("No coverage for test '" + testInfoBuilder.getUniformPath() + "'");
				continue;
			}
			report.tests.add(testInfo);
		}
		return report;
	}
}
