package eu.cqse.teamscale.jacoco.cache;

import eu.cqse.teamscale.jacoco.report.testwise.model.FileCoverage;
import org.jacoco.core.data.ExecutionData;

import java.util.HashMap;

/**
 * Holds {@link ClassCoverageLookup}s for all analyzed classes.
 */
public class ProbesCache {

	/** A mapping from class ID (CRC64 of the class file) to {@link ClassCoverageLookup}. */
	private final HashMap<Long, ClassCoverageLookup> classCoverageLookups = new HashMap<>();

	/** Adds a new class entry to the cache and returns its {@link ClassCoverageLookup}. */
	public ClassCoverageLookup createClass(long classId, String className) {
		ClassCoverageLookup classCoverageLookup = new ClassCoverageLookup(className);
		classCoverageLookups.put(classId, classCoverageLookup);
		return classCoverageLookup;
	}

	/** Returns whether a class with the given class ID has already been analyzed. */
	public boolean containsClassId(long classId) {
		return classCoverageLookups.containsKey(classId);
	}

	/**
	 * Converts the given {@link ExecutionData} to {@link FileCoverage} using the cached lookups or null if the class
	 * file of this class has not been included in the analysis or was not covered.
	 */
	public FileCoverage getCoverage(ExecutionData executionData) throws CoverageGenerationException {
		long classId = executionData.getId();
		if (!containsClassId(classId) || !executionData.hasHits()) {
			return null;
		}

		return classCoverageLookups.get(classId).getFileCoverage(executionData);
	}

	/** Returns true if the cache does not contains coverage for any class. */
	public boolean isEmpty() {
		return classCoverageLookups.isEmpty();
	}
}
