package eu.cqse.teamscale.jacoco.cache;

import eu.cqse.teamscale.jacoco.report.testwise.model.FileCoverage;
import org.jacoco.core.data.ExecutionData;

import java.util.HashMap;

public class ProbesCache {
    private final HashMap<Long, ProbeLookup> probeLookups = new HashMap<>();

    public ProbeLookup addClass(long classId, String className) {
        ProbeLookup probeLookup = new ProbeLookup(className);
        probeLookups.put(classId, probeLookup);
        return probeLookup;
    }

    public boolean containsClassId(long classId) {
        return probeLookups.containsKey(classId);
    }

    public FileCoverage getCoverage(ExecutionData executionData) {
        long classId = executionData.getId();
        if (!containsClassId(classId) || !executionData.hasHits()) {
            return null;
        }

        return probeLookups.get(classId).getFileCoverage(executionData);
    }

    public boolean isEmpty() {
        return probeLookups.isEmpty();
    }
}
