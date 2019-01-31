package com.teamscale.report.testwise.model

/** Holds coverage of a single file.  */
class FileCoverage(
    /** The name of the file.  */
    val fileName: String,
    /** A list of line ranges that have been covered.  */
    val coveredLines: String
)
