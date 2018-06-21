package eu.cqse.teamscale.jacoco.cache;

import eu.cqse.teamscale.jacoco.report.testwise.model.FileCoverage;
import eu.cqse.teamscale.jacoco.report.testwise.model.LineRange;
import org.conqat.lib.commons.string.StringUtils;
import org.jacoco.core.data.ExecutionData;

import java.util.ArrayList;
import java.util.List;

public class ProbeLookup {

    /** Fully qualified name of the class. */
    private String className;

    /** Name of the java source file. */
    private String sourceFileName;

    /**
     * List of method's line ranges. Each entry is associated with a probe at the same index.
     * So the same {@link LineRange}s can appear multiple times in the list if a method contains more than one probe.
     */
    private List<LineRange> probes = new ArrayList<>();

    private LineRange currentFile = new LineRange();

    private LineRange currentMethod = new LineRange();

    ProbeLookup(String className) {
        this.className = className;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public void addProbe(int probeId) {
        ensureArraySize(probeId);
        probes.set(probeId, currentMethod);
    }

    public void visitLine(int line) {
        currentFile.adjustToContain(line);
        currentMethod.adjustToContain(line);
    }

    private void ensureArraySize(int index) {
        while (index >= probes.size()) {
            probes.add(null);
        }
    }

    public FileCoverage getFileCoverage(ExecutionData executionData) {
        boolean[] executedProbes = executionData.getProbes();

        // Check probe invariant
        if (probes.size() > executedProbes.length) {
            throw new RuntimeException("Probe lookup does not match with actual probe size for " +
                    sourceFileName + " " + className + " (" + probes.size() + " vs " + executedProbes.length + ")!");
        }

        final FileCoverage fileCoverage = new FileCoverage(StringUtils.removeLastPart(className, '/'), sourceFileName);
        for (int i = 0; i < probes.size(); i++) {
            LineRange lineRange = probes.get(i);
            if (executedProbes[i] && lineRange != null) {
                fileCoverage.addLineRange(lineRange);
            }
        }

        return fileCoverage;
    }

    public void finishMethod() {
        currentMethod = new LineRange();
    }

}
