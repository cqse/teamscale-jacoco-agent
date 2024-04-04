package com.teamscale.profiler.installer.utils;

import spark.Request;
import spark.Response;
import spark.Service;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Mocks Teamscale. Returns a fixed status code for all requests.
 * By default: status 200
 */
public class MockTeamscale {

	private final Service service;

	private int statusCode = SC_OK;

	public MockTeamscale(int port) {
		service = Service.ignite();
		service.port(port);
		service.get("/*", (Request request, Response response) -> {
			response.status(statusCode);
			response.body("fake content");
			return response;
		});
		service.exception(Exception.class, (Exception exception, Request request, Response response) -> {
			response.status(SC_INTERNAL_SERVER_ERROR);
			response.body("Exception: " + exception.getMessage());
		});
		service.awaitInitialization();
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Shuts down the mock server and waits for it to be stopped.
	 */
	public void shutdown() {
		service.stop();
		service.awaitStop();
	}

}
