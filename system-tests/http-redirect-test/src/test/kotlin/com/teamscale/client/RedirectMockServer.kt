package com.teamscale.client

import spark.Request
import spark.Response
import spark.Service
import javax.servlet.http.HttpServletResponse

/**
 * Mocks a redirect server: redirects all requests to another server.
 */
class RedirectMockServer(port: Int, redirectTo: Int) {
	private val service = Service.ignite()

	init {
		service.port(port)
		service.post("/*") { request, response ->
			var url = request.url()
			url = url.replace(port.toString(), redirectTo.toString())
			response.redirect(url, 307)
			response
		}
		service.exception(Exception::class.java) { exception, _, response ->
			response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
			response.body("Exception: ${exception.message}")
		}
		service.awaitInitialization()
	}


	/**
	 * Shuts down the mock server and waits for it to be stopped.
	 */
	fun shutdown() {
		service.stop()
		service.awaitStop()
	}
}
