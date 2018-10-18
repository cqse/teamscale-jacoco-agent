package eu.cqse.teamscale.jacoco.agent.store;

/**
 * Exception thrown from an upload store. Either during the upload or in the validation process.
 */
public class UploadStoreException extends Exception {

	public UploadStoreException(String message, Exception e) {
		super(message, e);
	}

	public UploadStoreException(String message) {
		super(message);
	}
}
