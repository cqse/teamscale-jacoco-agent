package com.teamscale.report

/**
 * Behavior when two non-identical class files with the same package name are found.
 */
enum class EDuplicateClassFileBehavior {
	/** Completely ignores it.  */
	IGNORE,

	/** Prints a warning to the logger.  */
	WARN,

	/** Fails and stops further processing.  */
	FAIL
}
