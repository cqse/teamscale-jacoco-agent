package com.teamscale.report.testwise.jacoco.cache;

import com.teamscale.report.EDuplicateClassFileBehavior;
import com.teamscale.report.testwise.model.builder.FileCoverageBuilder;
import com.teamscale.report.util.ILogger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.report.JavaNames;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Holds {@link ClassCoverageLookup}s for all analyzed classes.
 */
public class ProbesCache {

	/** The logger. */
	private final ILogger logger;

	/** A mapping from class ID (CRC64 of the class file) to {@link ClassCoverageLookup}. */
	private final HashMap<Long, ClassCoverageLookup> classCoverageLookups = new HashMap<>();

	/** Holds all fully-qualified class names that are already contained in the cache. */
	private final Set<String> containedClasses = new HashSet<>();

	/** Whether to ignore non-identical duplicates of class files. */
	private final EDuplicateClassFileBehavior duplicateClassFileBehavior;

	private final ClassNotFoundLogger classNotFoundLogger;

	/** Constructor. */
	public ProbesCache(ILogger logger, EDuplicateClassFileBehavior duplicateClassFileBehavior) {
		this.logger = logger;
		this.classNotFoundLogger = new ClassNotFoundLogger(logger);
		this.duplicateClassFileBehavior = duplicateClassFileBehavior;
	}

	/** Adds a new class entry to the cache and returns its {@link ClassCoverageLookup}. */
	public ClassCoverageLookup createClass(long classId, String className) {
		if (containedClasses.contains(className)) {
			if (duplicateClassFileBehavior != EDuplicateClassFileBehavior.IGNORE) {
				logger.warn("Non-identical class file for class " + className + "."
						+ " This happens when a class with the same fully-qualified name is loaded twice but the two loaded class files are not identical."
						+ " A common reason for this is that the same library or shared code is included twice in your application but in two different versions."
						+ " The produced coverage for this class may not be accurate or may even be unusable."
						+ " To fix this problem, please resolve the conflict between both class files in your application.");
			}
			if (duplicateClassFileBehavior == EDuplicateClassFileBehavior.FAIL) {
				throw new IllegalStateException(
						"Found non-identical class file for class " + className + ". See logs for more details.");
			}
		}
		containedClasses.add(className);
		ClassCoverageLookup classCoverageLookup = new ClassCoverageLookup(className);
		classCoverageLookups.put(classId, classCoverageLookup);
		return classCoverageLookup;
	}

	/** Returns whether a class with the given class ID has already been analyzed. */
	public boolean containsClassId(long classId) {
		return classCoverageLookups.containsKey(classId);
	}

	/**
	 * Converts the given {@link ExecutionData} to {@link FileCoverageBuilder} using the cached lookups or null if the
	 * class file of this class has not been included in the analysis or was not covered.
	 */
	public FileCoverageBuilder getCoverage(ExecutionData executionData,
										   Predicate<String> locationIncludeFilter) throws CoverageGenerationException {
		long classId = executionData.getId();
		if (!containsClassId(classId)) {
			String fullyQualifiedClassName = new JavaNames().getQualifiedClassName(executionData.getName());
			if (locationIncludeFilter.test(fullyQualifiedClassName + ".class")) {
				classNotFoundLogger.log(fullyQualifiedClassName);
			}
			return null;
		}
		if (!executionData.hasHits()) {
			return null;
		}

		return classCoverageLookups.get(classId).getFileCoverage(executionData, logger);
	}

	/** Returns true if the cache does not contain coverage for any class. */
	public boolean isEmpty() {
		return classCoverageLookups.isEmpty();
	}

	/** Prints a the collected class not found messages. */
	public void flushLogger() {
		classNotFoundLogger.flush();
	}
}
