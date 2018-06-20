package eu.cqse.teamscale.jacoco.cache;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.jacoco.core.internal.analysis.CounterImpl;
import org.jacoco.core.internal.analysis.MethodCoverageImpl;
import org.jacoco.core.internal.data.CRC64;

import java.util.ArrayList;
import java.util.List;

public class ProbeLookup {

    /**
     * The class id used in the execution data ({@link CRC64} checksum of the class file).
     */
    private final long classId;

    /**
     * Fully qualified name of the class.
     */
    private String className;

    /**
     * Name of the java source file
     */
    private String sourceFileName;

    /**
     * List of all line number that have a probe attached to them.
     */
    private List<LineRange> probes = new ArrayList<>();

    private LineRange currentFile = new LineRange();

    private LineRange currentMethod = new LineRange();

    ProbeLookup(long classId, String className) {
        this.classId = classId;
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

    public ClassCoverageImpl getClassCoverage(ExecutionData executionData) {
        boolean[] executedProbes = executionData.getProbes();

        // Check probe invariant
        if (probes.size() > executedProbes.length) {
            throw new RuntimeException("Probe lookup does not match with actual probe size for " +
                    sourceFileName + " " + className + " (" + probes.size() + " vs " + executedProbes.length + ")!");
        }

        ArrayList<LineRange> coveredMethods = new ArrayList<>();
        for (int i = 0; i < probes.size(); i++) {
            if (executedProbes[i] && probes.get(i) != null) {
                LineRange range = probes.get(i);
                if (!coveredMethods.contains(range)) {
                    coveredMethods.add(range);
                }
            }
        }

        return buildClassCoverage(coveredMethods);
    }

    private ClassCoverageImpl buildClassCoverage(ArrayList<LineRange> coveredMethods) {
        final ClassCoverageImpl coverage = new ClassCoverageImpl(className, classId, false);
        coverage.setSourceFileName(sourceFileName == null ? className : sourceFileName);
        coverage.ensureCapacity(0, 1);
        for (LineRange coveredMethod : coveredMethods) {
            coverage.addMethod(new AggregatedMethodCoverage(coveredMethod));
        }

        if (!coveredMethods.isEmpty()) {
            // DUMMY coverage so that jacoco does not discard it as being empty
            coverage.increment(CounterImpl.COUNTER_0_1, CounterImpl.COUNTER_0_0, 0);
        }
        return coverage;
    }

    public void finishMethod() {
        currentMethod = new LineRange();
    }

    public static class AggregatedMethodCoverage extends MethodCoverageImpl {
        public LineRange range;

        public AggregatedMethodCoverage(LineRange range) {
            super("", "", "");
            this.range = range;
        }
    }

}
