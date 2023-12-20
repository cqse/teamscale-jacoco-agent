package com.teamscale.profiler.installer.utils;

import spark.Request;
import spark.Response;
import spark.Service;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Mocks a redirect server: redirects all requests to another server.
 */
public class MockTeamscale {

	private final Service service;

	public MockTeamscale(int port) {
		service = Service.ignite();
		service.port(port);
		service.get("/*", (Request request, Response response) -> {
			response.status(SC_OK);
			response.body("fake content");
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
