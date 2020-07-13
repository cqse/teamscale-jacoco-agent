package com.teamscale.jacoco.agent.options;

/**
 * Thrown if option parsing fails.
 */
public class AgentOptionParseException extends Exception {

	/**
	 * Serialization ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public AgentOptionParseException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 */
	public AgentOptionParseException(String message, Throwable cause) {
		super(message, cause);
	}

}
