package eu.cqse.teamscale.report.testwise.jacoco.cache;

import eu.cqse.teamscale.report.testwise.model.FileCoverage;
import eu.cqse.teamscale.report.util.ILogger;
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
	public FileCoverage getCoverage(ExecutionData executionData, ILogger logger) throws CoverageGenerationException {
		long classId = executionData.getId();
		if (!containsClassId(classId)) {
			logger.debug("Found coverage for a class " + classId + " that was not provided. Either you did not provide " +
					"all relevant class files or you did not adjust the include/exclude filters on the agent to exclude " +
					"coverage from irrelevant code.");
			return null;
		}
		if (!executionData.hasHits()) {
			return null;
		}

		return classCoverageLookups.get(classId).getFileCoverage(executionData, logger);
	}

	/** Returns true if the cache does not contains coverage for any class. */
	public boolean isEmpty() {
		return classCoverageLookups.isEmpty();
	}
}
