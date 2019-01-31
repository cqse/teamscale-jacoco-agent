package com.teamscale.report.testwise.model

import java.util.ArrayList

/** Container for coverage produced by multiple tests.  */
class TestwiseCoverageReport {

    /** The tests contained in the report.  */
    val tests: MutableList<TestInfo> = ArrayList()

}
