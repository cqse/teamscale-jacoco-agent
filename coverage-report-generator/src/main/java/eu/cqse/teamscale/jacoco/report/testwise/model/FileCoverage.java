package eu.cqse.teamscale.jacoco.report.testwise.model;

import eu.cqse.teamscale.jacoco.cache.LineRange;

import java.util.ArrayList;
import java.util.List;

class FileCoverage {

    public String fileName;

    public List<LineRange> ranges = new ArrayList<>();

    public FileCoverage(String fileName) {
        this.fileName = fileName;
    }
}
