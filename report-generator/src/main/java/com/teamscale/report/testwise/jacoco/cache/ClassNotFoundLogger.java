package com.teamscale.report.testwise.jacoco.cache;

import com.teamscale.report.util.ILogger;

import java.util.HashSet;
import java.util.Set;

/**
 * Coordinates logging of missing class files to ensure the warnings
 * are only emitted once and not for every individual test.
 */
/* package */ class ClassNotFoundLogger {

	/** The logger. */
	private final ILogger logger;

	/** Missing classes that will be logged when {@link #flush()} is called. */
	private Set<String> classesToBeLogged = new HashSet<>();

	/** Classes that have already been reported as missing. */
	private Set<String> alreadyLoggedClasses = new HashSet<>();

	/** Constructor */
	/* package */ ClassNotFoundLogger(ILogger logger) {
		this.logger = logger;
	}

	/** Saves the given class to be logged later on. Ensures that the class is only logged once. */
	/* package */ void log(String fullyQualifiedClassName) {
		if (!alreadyLoggedClasses.contains(fullyQualifiedClassName)) {
			classesToBeLogged.add(fullyQualifiedClassName);
		}
	}

	/* package */ void flush() {
		if (classesToBeLogged.isEmpty()) {
			return;
		}

		logger.warn(
				"Found coverage for " + classesToBeLogged
						.size() + " classes " + " that was not provided. Either you did not provide " +
						"all relevant class files or you did not adjust the include/exclude filters on the agent to exclude " +
						"coverage from irrelevant code. The classes are:"
		);
		for (String fullyQualifiedClassName : classesToBeLogged) {
			logger.warn(" - " + fullyQualifiedClassName);
		}
		alreadyLoggedClasses.addAll(classesToBeLogged);
		classesToBeLogged.clear();
	}
}
