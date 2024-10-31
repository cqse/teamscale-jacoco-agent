package com.teamscale.report.testwise.jacoco.cache

import com.teamscale.report.util.ILogger

/**
 * Coordinates logging of missing class files to ensure the warnings are only emitted once and not for every individual
 * test.
 */
internal class ClassNotFoundLogger(
	private val logger: ILogger
) {
	/** Missing classes that will be logged when [.flush] is called.  */
	private val classesToBeLogged = hashSetOf<String>()

	/** Classes that have already been reported as missing.  */
	private val alreadyLoggedClasses = hashSetOf<String>()

	/** Saves the given class to be logged later on. Ensures that the class is only logged once.  */ /* package */
	fun log(fullyQualifiedClassName: String) {
		if (alreadyLoggedClasses.contains(fullyQualifiedClassName)) return
		classesToBeLogged.add(fullyQualifiedClassName)
	}

	/** Writes a summary of the missing class files to the logger.  */ /* package */
	fun flush() {
		if (classesToBeLogged.isEmpty()) return

		logger.warn(
			"Found coverage for " + classesToBeLogged
				.size + " classes that were not provided. Either you did not provide " +
					"all relevant class files or you did not adjust the include/exclude filters on the agent to exclude " +
					"coverage from irrelevant code. The classes are:"
		)
		classesToBeLogged.forEach { fullyQualifiedClassName ->
			logger.warn(" - $fullyQualifiedClassName")
		}
		alreadyLoggedClasses.addAll(classesToBeLogged)
		classesToBeLogged.clear()
	}
}
