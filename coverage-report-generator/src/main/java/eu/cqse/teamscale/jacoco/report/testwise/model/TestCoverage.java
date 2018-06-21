package eu.cqse.teamscale.jacoco.report.testwise.model;

import java.util.HashMap;
import java.util.Map;

public class TestCoverage {

    public String externalId;

    public final Map<String, PathCoverage> paths = new HashMap<>();

    public TestCoverage(String externalId) {
        this.externalId = externalId;
    }

    public PathCoverage getOrCreatePath(String path) {
        return paths.computeIfAbsent(path, PathCoverage::new);
    }

}
