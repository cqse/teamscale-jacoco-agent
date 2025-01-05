package com.teamscale.profiler.installer.utils

import spark.Service
import javax.servlet.http.HttpServletResponse

/**
 * A mock HTTP server designed to simulate the behavior of a Teamscale server. It can be used in tests
 * to validate interactions with a Teamscale server without the need for an actual server instance.
 *
 * @constructor Initializes the mock server on the specified port and waits for it to start.
 * By default, it responds with a 200 OK status and a "fake content" response body.
 * Custom exceptions will return a 500 Internal Server Error response.
 *
 * @param port The port on which the mock server will be hosted.
 */
class MockTeamscale(port: Int) {
	private val service = Service.ignite()

	private var statusCode = HttpServletResponse.SC_OK

	init {
		service.port(port)
		service["/*", { _, response ->
			response.status(statusCode)
			response.body("fake content")
			response
		}]
		service.exception(
			Exception::class.java
		) { exception, _, response ->
			response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
			response.body("Exception: " + exception.message)
		}
		service.awaitInitialization()
	}

	fun setStatusCode(statusCode: Int) {
		this.statusCode = statusCode
	}

	/**
	 * Shuts down the mock server and waits for it to be stopped.
	 */
	fun shutdown() {
		service.stop()
		service.awaitStop()
	}
}
