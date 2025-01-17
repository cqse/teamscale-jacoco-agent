package com.teamscale.jacoco.agent.options;

/**
 * Thrown if option parsing fails.
 */
public class AgentOptionParseException extends Exception {

	/**
	 * Serialization ID.
	 */
	private static final long serialVersionUID = 1L;

	public AgentOptionParseException(String message) {
		super(message);
	}

	public AgentOptionParseException(Exception e) {
		super(e.getMessage(), e);
	}

	public AgentOptionParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
