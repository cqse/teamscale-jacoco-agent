package eu.cqse.teamscale.jacoco.common.cache;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionData;

import java.util.HashMap;

public class ProbesCache {
    private final HashMap<Long, ProbeLookup> probeLookups = new HashMap<>();

    public ProbeLookup addClass(long classId, String className) {
        ProbeLookup probeLookup = new ProbeLookup(classId, className);
        probeLookups.put(classId, probeLookup);
        return probeLookup;
    }

    public boolean containsClassId(long classId) {
        return probeLookups.containsKey(classId);
    }

    public IClassCoverage getCoverage(ExecutionData executionData) {
        long classId = executionData.getId();
        if (!containsClassId(classId) || !executionData.hasHits()) {
            return null;
        }

        return probeLookups.get(classId).getClassCoverage(executionData);
    }

    public boolean isEmpty() {
        return probeLookups.isEmpty();
    }
}
