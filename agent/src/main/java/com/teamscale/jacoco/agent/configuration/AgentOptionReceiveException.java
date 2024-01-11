package com.teamscale.jacoco.agent.configuration;

/** Thrown when retrieving the profiler configuration from Teamscale fails.  */
public class AgentOptionReceiveException extends Exception {

	/**
	 * Serialization ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public AgentOptionReceiveException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 */
	public AgentOptionReceiveException(String message, Throwable cause) {
		super(message, cause);
	}

}
