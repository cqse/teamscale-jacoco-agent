package com.teamscale.client;

/**
 * {@link TestDetails} with information about their partition as well as
 * tracking data used during prioritization of tests. Two instances are
 * considered equal if the test details are equals.
 */
public class PrioritizableTest {

	/** The uniform path the test (without -test- prefix). */
	public String uniformPath;

	/** The reason the test has been selected. */
	public String selectionReason;

}
