package eu.cqse.teamscale.test.controllers;

/**
 * Wrapper around errors originating from the {@link JaCoCoAgentController}.
 */
public class JacocoControllerError extends Error {

	/** Constructor. */
	public JacocoControllerError(String message, Throwable cause) {
		super(message, cause);
	}

	/** Constructor. */
	public JacocoControllerError(Throwable cause) {
		super(cause);
	}
}
