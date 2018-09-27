package eu.cqse.teamscale.report.testwise.model;

import org.conqat.lib.commons.collections.CollectionUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Container for coverage produced by multiple tests. */
@XmlRootElement(name = "report")
public class TestwiseCoverage {

	/** A mapping from test ID to {@link TestCoverage}. */
	private final Map<String, TestCoverage> tests = new HashMap<>();

	/**
	 * Adds the {@link TestCoverage} to the map.
	 * If there is already a test with the same ID the coverage is merged.
	 */
	public void add(TestCoverage coverage) {
		if (coverage == null || coverage.isEmpty()) {
			return;
		}
		if (tests.containsKey(coverage.externalId)) {
			TestCoverage testCoverage = tests.get(coverage.externalId);
			testCoverage.addAll(coverage.getFiles());
		} else {
			tests.put(coverage.externalId, coverage);
		}
	}

	/** Returns a collection of all tests contained in this container. */
	@XmlElement(name = "test")
	public List<TestCoverage> getTests() {
		return CollectionUtils.sort(tests.values());
	}

	/**
	 * Merges the given {@link TestwiseCoverage} with this one.
	 *
	 * @return Returns a reference to this
	 */
	public TestwiseCoverage merge(TestwiseCoverage testwiseCoverage) {
		for (TestCoverage value : testwiseCoverage.getTests()) {
			this.add(value);
		}
		return this;
	}
}
