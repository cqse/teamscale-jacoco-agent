package com.teamscale.profiler.installer.utils

import spark.Service
import javax.servlet.http.HttpServletResponse

/**
 * Mocks Teamscale. Returns a fixed status code for all requests.
 * By default: status 200
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
