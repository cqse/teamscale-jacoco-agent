package com.teamscale.client

/**
 * [TestDetails] with information about their partition. <br></br>
 * Note that two instances are considered equal if the test details are equal.
 */
class TestForPrioritization {

    /**
     * The uniform path the test.
     */
    var uniformPath: String? = null

    /** The reason the test has been selected.  */
    var selectionReason: String? = null

}
