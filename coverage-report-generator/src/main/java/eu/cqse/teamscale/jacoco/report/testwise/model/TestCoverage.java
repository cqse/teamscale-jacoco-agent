package eu.cqse.teamscale.jacoco.report.testwise.model;

import java.util.HashMap;
import java.util.Map;

/** Generic holder of test coverage of a single test based on line-ranges. */
public class TestCoverage {

    /** The external ID of the test. */
    public String externalId;

    /** Mapping from path names to all files on this path. */
    public final Map<String, PathCoverage> pathCoverageList = new HashMap<>();

    /** Constructor. */
    public TestCoverage(String externalId) {
        this.externalId = externalId;
    }

    /** Adds the {@link FileCoverage} to into the map,  but filters out empty coverage. */
    public void add(FileCoverage fileCoverage) {
        if (fileCoverage == null || fileCoverage.coveredRanges.isEmpty()) {
            return;
        }
        PathCoverage pathCoverage = pathCoverageList.computeIfAbsent(fileCoverage.path, PathCoverage::new);
        pathCoverage.add(fileCoverage);
    }
}
