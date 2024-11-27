package com.teamscale.tia.client

import com.teamscale.client.HttpUtils.getErrorBodyStringSafe
import retrofit2.Call
import java.io.IOException
import java.util.function.Supplier

/**
 * Utilities for performing requests to the agent.
 */
internal object AgentCommunicationUtils {
	/**
	 * Performs the given request and handles common errors (e.g., network failures, internal exceptions in the agent).
	 * In case of network problems, retries the request once.
	 */
	@Throws(AgentHttpRequestFailedException::class)
	fun <T> handleRequestError(errorMessage: String, requestFactory: () -> Call<T>) =
		handleRequestError(requestFactory, errorMessage, true)

	@Throws(AgentHttpRequestFailedException::class)
	private fun <T> handleRequestError(
		requestFactory: () -> Call<T>,
		errorMessage: String,
		retryOnce: Boolean
	): T? {
		try {
			val response = requestFactory().execute()
			if (response.isSuccessful) {
				return response.body()
			}

			val bodyString = getErrorBodyStringSafe(response)
			throw AgentHttpRequestFailedException(
				errorMessage + ". The agent responded with HTTP status " + response.code() + " " + response
					.message() + ". Response body: " + bodyString
			)
		} catch (e: IOException) {
			if (!retryOnce) {
				throw AgentHttpRequestFailedException(
					errorMessage + ". I already retried this request and it failed twice (see the suppressed" +
							" exception for details of the first failure). This is probably a network problem" +
							" that you should address.", e
				)
			}

			// retry once on network problems
			try {
				return handleRequestError(requestFactory, errorMessage, false)
			} catch (t: Throwable) {
				t.addSuppressed(e)
				throw t
			}
		}
	}
}
