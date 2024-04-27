package com.teamscale.report.jacoco

/**
 * Exception indicating that the generated report was empty and no {@link CoverageFile} was written to disk.
 */
class EmptyReportException(
	specificReason: String
) : Exception(
	specificReason +
		" Most likely you did not configure the agent correctly." +
		" Please check that the includes and excludes options are set correctly so the relevant code is included." +
		" If in doubt, first include more code and then iteratively narrow the patterns down to just the relevant code." +
		" If you have specified the class-dir option, please make sure it points to a directory containing the" +
		" class files/jars/wars/ears/etc. for which you are trying to measure code coverage."
)