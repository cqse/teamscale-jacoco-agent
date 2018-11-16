package eu.cqse.teamscale.report.testwise.model.builder;

import eu.cqse.teamscale.report.testwise.model.TestDetails;
import eu.cqse.teamscale.report.testwise.model.TestExecution;
import eu.cqse.teamscale.report.testwise.model.TestwiseCoverageReport;
import org.conqat.lib.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Container for coverage produced by multiple tests. */
public class TestwiseCoverageReportBuilder {

	/** A mapping from test ID to {@link TestCoverageBuilder}. */
	private final Map<String, TestInfoContainer> tests = new HashMap<>();

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
			TestInfoContainer container = new TestInfoContainer(testDetails.uniformPath);
			container.setDetails(testDetails);
			report.tests.put(testDetails.uniformPath, container);
		}
		for (TestCoverageBuilder coverage : testCoverage) {
			TestInfoContainer container = report.tests.get(coverage.getUniformPath());
			container.setCoverage(coverage);
		}
		for (TestExecution testExecution : testExecutions) {
			TestInfoContainer container = report.tests.get(testExecution.getUniformPath());
			container.setExecution(testExecution);
		}
		return report.build();
	}

	private TestwiseCoverageReport build() {
		TestwiseCoverageReport report = new TestwiseCoverageReport();
		List<TestInfoContainer> testInfoContainers = CollectionUtils
				.sort(tests.values(), Comparator.comparing(TestInfoContainer::getUniformPath));
		for (TestInfoContainer testInfoContainer : testInfoContainers) {
			report.tests.add(testInfoContainer.build());
		}
		return report;
	}
}
