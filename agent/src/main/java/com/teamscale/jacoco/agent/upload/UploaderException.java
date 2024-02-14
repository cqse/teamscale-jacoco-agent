package com.teamscale.jacoco.agent.upload;

import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;

/**
 * Exception thrown from an uploader. Either during the upload or in the validation process.
 */
public class UploaderException extends Exception {

	/** Constructor */
	public UploaderException(String message, Exception e) {
		super(message, e);
	}

	/** Constructor */
	public UploaderException(String message) {
		super(message);
	}

	/** Constructor */
	public UploaderException(String message, Response<ResponseBody> response) {
		super(createResponseMessage(message, response));
	}

	private static String createResponseMessage(String message, Response<ResponseBody> response) {
		int j= 0;
		try {
			String errorBodyMessage = response.errorBody().string();
			return String.format("%s (%s): \n%s", message, response.code(), errorBodyMessage);
		} catch (IOException | NullPointerException e) {
			int i = j+1;
			return i + "";
		}
	}
}
