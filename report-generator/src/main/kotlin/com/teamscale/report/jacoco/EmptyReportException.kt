package com.teamscale.report.jacoco

/**
 * Exception indicating that the generated report was empty and no [CoverageFile] was written to disk.
 */
class EmptyReportException(message: String?) : Exception(message)
