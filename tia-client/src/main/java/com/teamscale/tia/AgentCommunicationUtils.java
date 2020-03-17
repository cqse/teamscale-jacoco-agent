package com.teamscale.tia;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * Utilities for performing requests to the agent.
 */
class AgentCommunicationUtils {

	/**
	 * Performs the given request and handles common errors (e.g. network failures, internal exceptions in the agent).
	 * In case of network problems, retries the request once.
	 */
	static <T> T handleRequestError(Call<T> request, String errorMessage)
			throws AgentHttpRequestFailedException {
		return handleRequestError(request, errorMessage, true);
	}

	private static <T> T handleRequestError(Call<T> request, String errorMessage, boolean retryOnce)
			throws AgentHttpRequestFailedException {

		try {
			Response<T> response = request.execute();
			if (response.isSuccessful()) {
				return response.body();
			}

			ResponseBody errorBody = response.errorBody();
			String bodyString = "<no response body sent>";
			if (errorBody != null) {
				bodyString = errorBody.string();
			}
			throw new AgentHttpRequestFailedException(
					errorMessage + ". The agent responded with HTTP status " + response.code() + " " + response
							.message() + ". Response body: " + bodyString);
		} catch (IOException e) {
			if (!retryOnce) {
				throw new AgentHttpRequestFailedException(
						errorMessage + ". I already retried this request and it failed twice (see the suppressed" +
								" exception for details of the first failure). This is probably a network problem" +
								" that you should address.", e);
			}

			// retry once on network problems
			try {
				return handleRequestError(request, errorMessage, false);
			} catch (Throwable t) {
				t.addSuppressed(e);
				throw t;
			}
		}
	}

}
