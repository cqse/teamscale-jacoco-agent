package eu.cqse.teamscale.jacoco.report.testwise.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
    public Collection<TestCoverage> getTests() {
        return tests.values();
    }
}
