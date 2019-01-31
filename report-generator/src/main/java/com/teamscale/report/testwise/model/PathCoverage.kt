package com.teamscale.report.testwise.model

/** Container for [FileCoverage]s of the same path.  */
class PathCoverage
/** Constructor.  */
    (
    /** File system path.  */
    val path: String,
    /** Files with coverage.  */
    val files: List<FileCoverage>
)
