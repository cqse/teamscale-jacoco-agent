package com.teamscale.report.testwise.model.builder;

import com.teamscale.client.TestDetails;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.TestwiseCoverageReport;
import org.conqat.lib.commons.collections.CollectionUtils;

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
	 * Adds the {@link TestCoverageBuilder} to the map.
	 * If there is already a test with the same ID the coverage is merged.
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
			TestInfoBuilder container = report.tests.get(coverage.getUniformPath());
			container.setCoverage(coverage);
		}
		for (TestExecution testExecution : testExecutions) {
			TestInfoBuilder container = report.tests.get(testExecution.getUniformPath());
			container.setExecution(testExecution);
		}
		return report.build();
	}

	private TestwiseCoverageReport build() {
		TestwiseCoverageReport report = new TestwiseCoverageReport();
		List<TestInfoBuilder> testInfoBuilders = CollectionUtils
				.sort(tests.values(), Comparator.comparing(TestInfoBuilder::getUniformPath));
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
