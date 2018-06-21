package eu.cqse.teamscale.jacoco.report.testwise.model;

import java.util.HashMap;
import java.util.Map;

public class PathCoverage {

    public String path;

    public Map<String, FileCoverage> fileCoverageList = new HashMap<>();

    public PathCoverage(String path) {
        this.path = path;
    }

    public void add(FileCoverage fileCoverage) {
        if(fileCoverageList.containsKey(fileCoverage.fileName)) {
            FileCoverage existingFile = fileCoverageList.get(fileCoverage.fileName);
            existingFile.merge(fileCoverage.ranges);
        } else {
            fileCoverageList.put(fileCoverage.fileName, fileCoverage);
        }
    }
}
