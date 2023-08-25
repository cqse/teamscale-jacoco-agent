package com.teamscale.client;

import spark.Request;
import spark.Response;
import spark.Service;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

/**
 * Mocks a redirect server: redirects all requests to another server.
 */
public class RedirectMockServer {

	private final Service service;

	public RedirectMockServer(int port, int redirectTo) {
		service = Service.ignite();
		service.port(port);
		service.post("/*", (Request request, Response response) -> {
			String url = request.url();
			url = url.replace(Integer.toString(port), Integer.toString(redirectTo));
			response.redirect(url, 307);
			return response;
		});
		service.exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
		service.awaitInitialization();
	}


	/**
	 * Shuts down the mock server and waits for it to be stopped.
	 */
	public void shutdown() {
		service.stop();
		service.awaitStop();
	}

}
