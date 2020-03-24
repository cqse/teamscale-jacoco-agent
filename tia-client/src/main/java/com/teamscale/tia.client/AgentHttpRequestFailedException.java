package com.teamscale.tia.client;

/**
 * Thrown if communicating with the agent via HTTP fails. The underlying reason can be either a network problem or an
 * internal error in the agent. Users of this library should report these exceptions appropriately so the underlying
 * problems can be addressed.
 */
public class AgentHttpRequestFailedException extends Exception {

	public AgentHttpRequestFailedException(String message) {
		super(message);
	}

	public AgentHttpRequestFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
