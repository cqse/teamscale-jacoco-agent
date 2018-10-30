package eu.cqse.teamscale.jacoco.agent.store;

/**
 * Exception thrown from an upload store. Either during the upload or in the validation process.
 */
public class UploadStoreException extends Exception {

	/** Constructor */
	public UploadStoreException(String message, Exception e) {
		super(message, e);
	}

	/** Constructor */
	public UploadStoreException(String message) {
		super(message);
	}
}
