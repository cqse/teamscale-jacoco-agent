package com.teamscale.tia;

public class AgentHttpRequestFailedException extends Exception {

	public AgentHttpRequestFailedException(String message) {
		super(message);
	}

	public AgentHttpRequestFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
