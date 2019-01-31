package com.teamscale.report.testwise.closure.model

/** Model for the google closure coverage format.  */
class ClosureCoverage(
    /** The uniformPath of the test that produced the coverage.  */
    val uniformPath: String,
    /** Holds a list of all covered js files with absolute path.  */
    val fileNames: List<String>,
    /**
     * Holds for each line in each file a list with true or null
     * to indicate if the line has been executed.
     */
    val executedLines: List<List<Boolean>>
)
