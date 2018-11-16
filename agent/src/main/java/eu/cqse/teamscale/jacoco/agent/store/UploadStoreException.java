package eu.cqse.teamscale.jacoco.agent.store;

import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;

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

	/** Constructor */
	public UploadStoreException(String message, Response<ResponseBody> response) {
		super(createResponseMessage(message, response));
	}

	private static String createResponseMessage(String message, Response<ResponseBody> response) {
		try {
			String errorBodyMessage = response.errorBody().string();
			return String.format("%s (%s): \n%s", message, response.code(), errorBodyMessage);
		} catch (IOException | NullPointerException e) {
			return message;
		}
	}
}
