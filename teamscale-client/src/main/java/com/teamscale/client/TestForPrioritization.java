package com.teamscale.client;

/**
 * {@link TestDetails} with information about their partition. <br/>
 * Note that two instances are considered equal if the test details are equal.
 */
public class TestForPrioritization {

	/**
	 * The uniform path the test.
	 */
	public String uniformPath;

	/** The reason the test has been selected. */
	public String selectionReason;

}
