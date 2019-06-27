package com.teamscale.report.testwise.model.factory;

import com.teamscale.client.TestDetails;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestInfoBuilder;
import com.teamscale.report.testwise.model.builder.TestwiseCoverageReportBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Factory class for converting {@link TestCoverageBuilder} to {@link TestInfo}s while augmenting them with information
 * from test details and test executions.
 */
public class TestInfoFactory {

	/** Maps uniform paths to test details. */
	private Map<String, TestDetails> testDetailsMap = new HashMap<>();

	/** Maps uniform paths to test executions. */
	private Map<String, TestExecution> testExecutionsMap = new HashMap<>();

	/** Holds all uniform paths for tests that have been written to the outputFile. */
	private final Set<String> uniformPathsWithCoverage = new HashSet<>();

	public TestInfoFactory(List<TestDetails> testDetails, List<TestExecution> testExecutions) {
		for (TestDetails testDetail : testDetails) {
			testDetailsMap.put(testDetail.uniformPath, testDetail);
		}
		for (TestExecution testExecution : testExecutions) {
			testExecutionsMap.put(testExecution.getUniformPath(), testExecution);
		}
	}

	/**
	 * Converts the given {@link TestCoverageBuilder} to a {@link TestInfo} using the internally stored test details and
	 * test executions.
	 */
	public TestInfo createFor(TestCoverageBuilder testCoverageBuilder) {
		String resolvedUniformPath = resolveUniformPath(testCoverageBuilder.getUniformPath());
		uniformPathsWithCoverage.add(resolvedUniformPath);

		TestInfoBuilder container = new TestInfoBuilder(resolvedUniformPath);
		container.setCoverage(testCoverageBuilder);
		TestDetails testDetails = testDetailsMap.get(resolvedUniformPath);
		if (testDetails == null) {
			System.err.println("No test details found for " + resolvedUniformPath);
		}
		container.setDetails(testDetails);
		TestExecution execution = testExecutionsMap.get(resolvedUniformPath);
		if (execution == null) {
			System.err.println("No test execution found for " + resolvedUniformPath);
		}
		container.setExecution(execution);
		return container.build();
	}

	/** Returns {@link TestInfo}s for all tests that have not been used yet in {@link #createFor(TestCoverageBuilder)}. */
	public List<TestInfo> createTestInfosWithoutCoverage() {
		ArrayList<TestInfo> results = new ArrayList<>();
		for (TestDetails testDetails : testDetailsMap.values()) {
			if (uniformPathsWithCoverage.contains(testDetails.uniformPath)) {
				TestInfoBuilder testInfo = new TestInfoBuilder(testDetails.uniformPath);
				testInfo.setDetails(testDetails);
				testInfo.setExecution(testExecutionsMap.get(testDetails.uniformPath));
				results.add(testInfo.build());
			}
		}
		return results;
	}

	/**
	 * Strips parameterized test arguments when the full path given in the coverage file cannot be found in the test
	 * details.
	 */
	private String resolveUniformPath(String originalUniformPath) {
		String uniformPath = originalUniformPath;
		TestDetails testDetails = testDetailsMap.get(uniformPath);
		if (testDetails == null) {
			uniformPath = TestwiseCoverageReportBuilder
					.stripParameterizedTestArguments(uniformPath);
		}
		return uniformPath;
	}
}
