package com.teamscale.tia;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public class AgentCommunicationUtils {

	static <T> T handleRequestError(Call<T> request,
									String errorMessage) throws AgentHttpRequestFailedException {
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
			throw new AgentHttpRequestFailedException(
					errorMessage + ". This is probably a temporary network problem.", e);
		}
	}

}
