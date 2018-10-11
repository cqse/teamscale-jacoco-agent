package eu.cqse.teamscale.report.testwise.model;

import org.conqat.lib.commons.collections.CollectionUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Container for coverage produced by multiple tests. */
@XmlRootElement(name = "report")
public class TestwiseCoverageReport {

	/** A mapping from test ID to {@link TestCoverage}. */
	private final Map<String, TestInfoContainer> tests = new HashMap<>();

	/**
	 * Adds the {@link TestCoverage} to the map.
	 * If there is already a test with the same ID the coverage is merged.
	 */
	public static TestwiseCoverageReport createFrom(
			Collection<TestDetails> testDetailsList,
			Collection<TestCoverage> testCoverage,
			Collection<TestExecution> testExecutions
	) {
		TestwiseCoverageReport report = new TestwiseCoverageReport();
		for (TestDetails testDetails : testDetailsList) {
			TestInfoContainer container = new TestInfoContainer(testDetails.uniformPath);
			container.setDetails(testDetails);
			report.tests.put(testDetails.uniformPath, container);
		}
		for (TestCoverage coverage : testCoverage) {
			TestInfoContainer container = report.tests.get(coverage.getUniformPath());
			container.setCoverage(coverage);
		}
		for (TestExecution testExecution : testExecutions) {
			TestInfoContainer container = report.tests.get(testExecution.getUniformPath());
			container.setExecution(testExecution);
		}
		return report;
	}

	/** Returns a collection of all tests contained in this report. */
	@XmlElement(name = "test")
	public List<TestInfoContainer> getTests() {
		return CollectionUtils.sort(tests.values(), Comparator.comparing(TestInfoContainer::getUniformPath));
	}
}
